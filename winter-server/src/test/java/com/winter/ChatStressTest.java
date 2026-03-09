package com.winter;

import com.google.protobuf.ByteString;
import com.winter.core.util.GameExecutor;
import com.winter.modules.chat.util.SensitiveWordFilter;
import com.winter.modules.player.PlayerManager;
import com.winter.common.model.PlayerModel;
import com.winter.msg.ChatMsg;
import com.winter.msg.MsgId.CmdId;
import com.winter.msg.PacketMsg.GamePacket;

import io.netty.channel.*;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.bootstrap.Bootstrap;
import io.netty.util.AttributeKey;

import org.junit.jupiter.api.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 万人聊天压力测试
 * <p>
 * 不需要真正启动服务器和客户端 TCP 连接，
 * 通过模拟 Channel + PlayerManager 注册来测试聊天链路的核心性能瓶颈：
 * <p>
 * 1. 广播性能测试: 1 万个 Channel，测试 broadcast() 的吞吐量和延迟
 * 2. 私聊吞吐测试: 1 万人同时发私聊消息，测试 sendToPlayer() 吞吐
 * 3. 敏感词过滤性能: 大词库下的过滤延迟
 * 4. 业务线程池压力: 大量并发任务提交到 GameExecutor
 * 5. 综合场景: 模拟万人同时发私聊+世界广播的混合场景
 */
public class ChatStressTest {

    private static final int PLAYER_COUNT = 10_000;           // 模拟万人
    private static final int BROADCAST_ROUNDS = 100;          // 广播轮次
    private static final int PRIVATE_CHAT_COUNT = 50_000;     // 私聊总条数
    private static final int CONCURRENT_SENDERS = 1_000;      // 并发发送者数

    private static EventLoopGroup eventLoopGroup;
    private static List<Channel> channels = new ArrayList<>(PLAYER_COUNT);
    private static final AtomicLong writeCount = new AtomicLong(0);

    /**
     * 一个轻量级的 Mock Channel，只统计 write 调用次数，不真正做网络 I/O
     */
    static class MockChannel extends EmbeddedChannel {
        private final AtomicLong counter;

        MockChannel(AtomicLong counter) {
            super(new ChannelOutboundHandlerAdapter() {
                @Override
                public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
                    counter.incrementAndGet();
                    promise.setSuccess(); // 直接标记成功，不做真正写入
                }
            });
            this.counter = counter;
        }
    }

    @BeforeAll
    static void setup() {
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║       WinterServer 万人聊天压力测试              ║");
        System.out.println("╠══════════════════════════════════════════════════╣");
        System.out.println("║  模拟玩家数: " + PLAYER_COUNT + "                          ║");
        System.out.println("║  测试环境: 内存模拟 Channel (无真实网络 I/O)      ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
        System.out.println();

        // 注册 PLAYER_COUNT 个模拟玩家
        System.out.print("正在注册 " + PLAYER_COUNT + " 个模拟玩家...");
        long start = System.currentTimeMillis();

        for (int i = 1; i <= PLAYER_COUNT; i++) {
            EmbeddedChannel ch = new EmbeddedChannel(new ChannelOutboundHandlerAdapter() {
                @Override
                public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
                    writeCount.incrementAndGet();
                    promise.setSuccess();
                }
            });
            channels.add(ch);

            PlayerModel player = new PlayerModel(i);
            player.setName("Player_" + i);
            PlayerManager.addPlayer(player, ch);
        }

        long elapsed = System.currentTimeMillis() - start;
        System.out.println(" 完成! (" + elapsed + "ms)");
        System.out.println("当前在线人数: " + PlayerManager.getOnlineCount());
        System.out.println();
    }

    @AfterAll
    static void teardown() {
        // 清理所有 Channel
        for (Channel ch : channels) {
            try {
                ch.close();
            } catch (Exception ignored) {
            }
        }
        System.out.println("\n测试完成，资源已清理。");
    }

    // ═══════════════════════════════════════════════════════════════
    // 测试 1: 世界广播吞吐量
    // ═══════════════════════════════════════════════════════════════
    @Test
    @DisplayName("测试1: 世界广播 - " + PLAYER_COUNT + " 人 × " + BROADCAST_ROUNDS + " 轮")
    void testBroadcastThroughput() throws Exception {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("测试1: 世界广播吞吐量");
        System.out.println("  场景: 1 个玩家发送世界消息，推送给 " + PLAYER_COUNT + " 人");
        System.out.println("  轮次: " + BROADCAST_ROUNDS);
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        writeCount.set(0);

        // 构造一条广播消息
        ChatMsg.BrdGroupChat chatMsg = ChatMsg.BrdGroupChat.newBuilder()
                .setFromId(1)
                .setFromName("TestPlayer")
                .setContent("这是一条万人广播测试消息，用于验证服务器的广播性能！")
                .setMsgType(0)
                .setTimestamp(System.currentTimeMillis())
                .build();

        // 预热
        PlayerManager.broadcast(chatMsg, CmdId.BRD_BROADCAST_CHAT);
        Thread.sleep(500);
        writeCount.set(0);

        // 正式测试
        long start = System.nanoTime();
        for (int i = 0; i < BROADCAST_ROUNDS; i++) {
            PlayerManager.broadcast(chatMsg, CmdId.BRD_BROADCAST_CHAT);
        }

        // EmbeddedChannel 的 EventLoop 不会自动执行排队任务，需手动触发
        for (Channel ch : channels) {
            ((EmbeddedChannel) ch).runPendingTasks();
        }
        long elapsedNs = System.nanoTime() - start;

        long totalWrites = writeCount.get();
        double elapsedMs = elapsedNs / 1_000_000.0;
        double avgPerRound = elapsedMs / BROADCAST_ROUNDS;
        long expectedWrites = (long) BROADCAST_ROUNDS * PLAYER_COUNT;

        System.out.println();
        System.out.println("  ┌─────────────────────────────────────────┐");
        System.out.printf("  │ 总耗时:        %10.1f ms             │%n", elapsedMs);
        System.out.printf("  │ 平均每轮:      %10.2f ms             │%n", avgPerRound);
        System.out.printf("  │ 实际写入次数:  %,10d                  │%n", totalWrites);
        System.out.printf("  │ 期望写入次数:  %,10d                  │%n", expectedWrites);
        System.out.printf("  │ 写入吞吐量:    %,.0f msg/s            │%n", totalWrites / (elapsedMs / 1000.0));
        System.out.println("  └─────────────────────────────────────────┘");

        // 断言：写入次数应接近预期（EmbeddedChannel 是同步的，应该完全一致）
        Assertions.assertTrue(totalWrites >= expectedWrites * 0.95,
                "广播写入次数不足，期望 " + expectedWrites + "，实际 " + totalWrites);
        // 断言：平均单轮广播应在合理时间内
        Assertions.assertTrue(avgPerRound < 500,
                "单轮广播耗时过长: " + avgPerRound + "ms（阈值 500ms）");
    }

    // ═══════════════════════════════════════════════════════════════
    // 测试 2: 私聊吞吐量（多对多并发）
    // ═══════════════════════════════════════════════════════════════
    @Test
    @DisplayName("测试2: 私聊吞吐 - " + CONCURRENT_SENDERS + " 并发 × " + PRIVATE_CHAT_COUNT + " 条")
    void testPrivateChatThroughput() throws Exception {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("测试2: 私聊吞吐量");
        System.out.println("  场景: " + CONCURRENT_SENDERS + " 人同时发私聊，共 " + PRIVATE_CHAT_COUNT + " 条");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        writeCount.set(0);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        ExecutorService senderPool = Executors.newFixedThreadPool(CONCURRENT_SENDERS);
        CountDownLatch latch = new CountDownLatch(PRIVATE_CHAT_COUNT);
        Random random = new Random(42);

        long start = System.nanoTime();

        for (int i = 0; i < PRIVATE_CHAT_COUNT; i++) {
            final long senderId = random.nextInt(PLAYER_COUNT) + 1;
            long targetId = random.nextInt(PLAYER_COUNT) + 1;
            while (targetId == senderId) {
                targetId = random.nextInt(PLAYER_COUNT) + 1;
            }
            final long finalTargetId = targetId;

            senderPool.submit(() -> {
                try {
                    ChatMsg.BrdPrivateChat chatMsg = ChatMsg.BrdPrivateChat.newBuilder()
                            .setFromId(senderId)
                            .setFromName("Player_" + senderId)
                            .setContent("你好，这是一条测试私聊消息 #" + senderId)
                            .setMsgType(0)
                            .setTimestamp(System.currentTimeMillis())
                            .build();
                    PlayerManager.sendToPlayer(finalTargetId, chatMsg, CmdId.BRD_PRIVATE_CHAT);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        long elapsedNs = System.nanoTime() - start;
        senderPool.shutdown();

        double elapsedMs = elapsedNs / 1_000_000.0;
        long totalWrites = writeCount.get();

        System.out.println();
        System.out.println("  ┌─────────────────────────────────────────┐");
        System.out.printf("  │ 总耗时:        %10.1f ms             │%n", elapsedMs);
        System.out.printf("  │ 成功发送:      %,10d                  │%n", successCount.get());
        System.out.printf("  │ 失败发送:      %,10d                  │%n", failCount.get());
        System.out.printf("  │ 实际写入:      %,10d                  │%n", totalWrites);
        System.out.printf("  │ 吞吐量:        %,.0f msg/s            │%n", successCount.get() / (elapsedMs / 1000.0));
        System.out.println("  └─────────────────────────────────────────┘");

        Assertions.assertEquals(0, failCount.get(), "不应有失败的发送");
        Assertions.assertTrue(elapsedMs < 30_000, "私聊测试不应超过 30 秒");
    }

    // ═══════════════════════════════════════════════════════════════
    // 测试 3: 敏感词过滤性能
    // ═══════════════════════════════════════════════════════════════
    @Test
    @DisplayName("测试3: 敏感词过滤 - 万次过滤性能")
    void testSensitiveWordFilterPerformance() {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("测试3: 敏感词过滤性能 (AC 自动机)");
        System.out.println("  场景: 加载 1000 个敏感词，过滤 10000 条消息");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        SensitiveWordFilter filter = new SensitiveWordFilter();

        // 模拟加载 1000 个敏感词
        List<String> sensitiveWords = new ArrayList<>();
        String[] bases = {"暴力", "赌博", "色情", "毒品", "诈骗", "谩骂", "侮辱", "歧视", "恐怖", "极端",
                "违法", "犯罪", "洗钱", "传销", "邪教", "反动", "颠覆", "分裂", "暗杀", "走私"};
        for (int i = 0; i < 1000; i++) {
            sensitiveWords.add(bases[i % bases.length] + "_变体_" + i);
        }
        filter.addWords(sensitiveWords);
        filter.buildFailPointers();

        // 构造测试消息（部分包含敏感词）
        String[] testMessages = new String[10_000];
        Random random = new Random(42);
        for (int i = 0; i < testMessages.length; i++) {
            if (i % 10 == 0) {
                // 10% 的消息包含敏感词
                testMessages[i] = "这是一条正常消息，但包含了" + sensitiveWords.get(random.nextInt(1000)) + "哦，别说了";
            } else {
                testMessages[i] = "这是一条完全正常的聊天消息，没有任何问题 #" + i + "，我在打怪升级！冬天太冷了。";
            }
        }

        // 预热
        for (int i = 0; i < 100; i++) {
            filter.filter(testMessages[i]);
        }

        // 正式测试
        long start = System.nanoTime();
        int hitCount = 0;
        for (String msg : testMessages) {
            String filtered = filter.filter(msg);
            if (!filtered.equals(msg)) {
                hitCount++;
            }
        }
        long elapsedNs = System.nanoTime() - start;

        double elapsedMs = elapsedNs / 1_000_000.0;
        double avgUs = (elapsedNs / testMessages.length) / 1000.0;

        System.out.println();
        System.out.println("  ┌─────────────────────────────────────────┐");
        System.out.printf("  │ 敏感词数量:    %,10d                  │%n", sensitiveWords.size());
        System.out.printf("  │ 过滤消息数:    %,10d                  │%n", testMessages.length);
        System.out.printf("  │ 命中消息数:    %,10d                  │%n", hitCount);
        System.out.printf("  │ 总耗时:        %10.1f ms             │%n", elapsedMs);
        System.out.printf("  │ 平均每条:      %10.1f μs             │%n", avgUs);
        System.out.printf("  │ 吞吐量:        %,.0f msg/s            │%n", testMessages.length / (elapsedMs / 1000.0));
        System.out.println("  └─────────────────────────────────────────┘");

        Assertions.assertTrue(avgUs < 1000, "单条过滤应 < 1ms，实际 " + avgUs + "μs");
        Assertions.assertTrue(hitCount > 0, "应有命中敏感词的消息");
    }

    // ═══════════════════════════════════════════════════════════════
    // 测试 4: 业务线程池压力测试
    // ═══════════════════════════════════════════════════════════════
    @Test
    @DisplayName("测试4: 业务线程池 - 万级任务并发提交")
    void testGameExecutorUnderPressure() throws Exception {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("测试4: GameExecutor 业务线程池压力测试");
        System.out.println("  场景: " + PLAYER_COUNT + " 个任务并发提交，每个模拟聊天处理流程");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        int taskCount = PLAYER_COUNT;
        CountDownLatch latch = new CountDownLatch(taskCount);
        AtomicInteger completed = new AtomicInteger(0);
        AtomicInteger rejected = new AtomicInteger(0);

        // 构造一个敏感词过滤器（模拟真实业务流程）
        SensitiveWordFilter filter = new SensitiveWordFilter();
        filter.addWords(Arrays.asList("测试敏感词1", "测试敏感词2", "测试敏感词3"));
        filter.buildFailPointers();

        long start = System.nanoTime();

        for (int i = 0; i < taskCount; i++) {
            final int idx = i;
            try {
                GameExecutor.execute(() -> {
                    try {
                        // 模拟聊天处理流程：敏感词过滤 + 构建消息 + 发送
                        String msg = "玩家" + idx + "说：大家好，我是新人，今天天气不错！测试敏感词1存在";
                        String filtered = filter.filter(msg);

                        ChatMsg.BrdPrivateChat chatMsg = ChatMsg.BrdPrivateChat.newBuilder()
                                .setFromId(idx + 1)
                                .setFromName("Player_" + (idx + 1))
                                .setContent(filtered)
                                .setMsgType(0)
                                .setTimestamp(System.currentTimeMillis())
                                .build();

                        // 随机发给某个在线玩家
                        long targetId = (idx % PLAYER_COUNT) + 1;
                        if (targetId == idx + 1) targetId = (targetId % PLAYER_COUNT) + 1;
                        PlayerManager.sendToPlayer(targetId, chatMsg, CmdId.BRD_PRIVATE_CHAT);

                        completed.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            } catch (Exception e) {
                rejected.incrementAndGet();
                latch.countDown();
            }
        }

        boolean finished = latch.await(30, TimeUnit.SECONDS);
        long elapsedNs = System.nanoTime() - start;
        double elapsedMs = elapsedNs / 1_000_000.0;

        System.out.println();
        System.out.println("  ┌─────────────────────────────────────────┐");
        System.out.printf("  │ 提交任务数:    %,10d                  │%n", taskCount);
        System.out.printf("  │ 完成任务数:    %,10d                  │%n", completed.get());
        System.out.printf("  │ 被拒绝任务:    %,10d                  │%n", rejected.get());
        System.out.printf("  │ 总耗时:        %10.1f ms             │%n", elapsedMs);
        System.out.printf("  │ 吞吐量:        %,.0f task/s           │%n", completed.get() / (elapsedMs / 1000.0));
        System.out.printf("  │ 线程池状态:    %s │%n", GameExecutor.getStatus());
        System.out.printf("  │ 全部完成:      %s                       │%n", finished ? "✓" : "✗ 超时!");
        System.out.println("  └─────────────────────────────────────────┘");

        Assertions.assertTrue(finished, "应在 30 秒内完成所有任务");
        Assertions.assertEquals(0, rejected.get(), "不应有被拒绝的任务");
        Assertions.assertEquals(taskCount, completed.get(), "所有任务应完成");
    }

    // ═══════════════════════════════════════════════════════════════
    // 测试 5: 综合场景 - 万人混合聊天（私聊 + 广播 同时进行）
    // ═══════════════════════════════════════════════════════════════
    @Test
    @DisplayName("测试5: 综合场景 - 万人混合聊天 (私聊+广播)")
    void testMixedChatScenario() throws Exception {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("测试5: 综合场景 - 万人混合聊天");
        System.out.println("  场景: 同时进行 10 次世界广播 + 10000 条私聊");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        writeCount.set(0);
        int broadcastCount = 10;
        int privateMsgCount = 10_000;

        CountDownLatch latch = new CountDownLatch(privateMsgCount);
        AtomicInteger broadcastDone = new AtomicInteger(0);
        AtomicInteger privateDone = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);

        ExecutorService pool = Executors.newFixedThreadPool(200);
        Random random = new Random(123);

        long start = System.nanoTime();

        // 在主线程顺序执行广播（EmbeddedEventLoop 非线程安全，并发 execute 会导致竞态）
        for (int i = 0; i < broadcastCount; i++) {
            ChatMsg.BrdGroupChat msg = ChatMsg.BrdGroupChat.newBuilder()
                    .setFromId(i + 1)
                    .setFromName("Broadcaster_" + i)
                    .setContent("第 " + i + " 轮世界广播！所有人都能看到这条消息。")
                    .setMsgType(0)
                    .setTimestamp(System.currentTimeMillis())
                    .build();
            PlayerManager.broadcast(msg, CmdId.BRD_BROADCAST_CHAT);
            broadcastDone.incrementAndGet();
        }
        // 立即执行排队的广播写任务
        for (Channel ch : channels) {
            ((EmbeddedChannel) ch).runPendingTasks();
        }

        // 提交私聊任务
        for (int i = 0; i < privateMsgCount; i++) {
            final long senderId = random.nextInt(PLAYER_COUNT) + 1;
            long targetId = random.nextInt(PLAYER_COUNT) + 1;
            while (targetId == senderId) targetId = random.nextInt(PLAYER_COUNT) + 1;
            final long fTarget = targetId;

            pool.submit(() -> {
                try {
                    ChatMsg.BrdPrivateChat msg = ChatMsg.BrdPrivateChat.newBuilder()
                            .setFromId(senderId)
                            .setFromName("P_" + senderId)
                            .setContent("混合压测私聊消息")
                            .setMsgType(0)
                            .setTimestamp(System.currentTimeMillis())
                            .build();
                    PlayerManager.sendToPlayer(fTarget, msg, CmdId.BRD_PRIVATE_CHAT);
                    privateDone.incrementAndGet();
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean finished = latch.await(60, TimeUnit.SECONDS);
        // EmbeddedChannel 的 EventLoop 不会自动执行排队任务，需手动触发
        for (Channel ch : channels) {
            ((EmbeddedChannel) ch).runPendingTasks();
        }
        long elapsedNs = System.nanoTime() - start;
        pool.shutdown();

        double elapsedMs = elapsedNs / 1_000_000.0;
        long totalWrites = writeCount.get();
        long expectedMin = privateMsgCount + (long) broadcastCount * PLAYER_COUNT;

        System.out.println();
        System.out.println("  ┌─────────────────────────────────────────┐");
        System.out.printf("  │ 总耗时:        %10.1f ms             │%n", elapsedMs);
        System.out.printf("  │ 广播完成:      %,10d / %-5d          │%n", broadcastDone.get(), broadcastCount);
        System.out.printf("  │ 私聊完成:      %,10d / %-5d          │%n", privateDone.get(), privateMsgCount);
        System.out.printf("  │ 错误数:        %,10d                  │%n", errors.get());
        System.out.printf("  │ 总写入次数:    %,10d                  │%n", totalWrites);
        System.out.printf("  │ 期望最少写入:  %,10d                  │%n", expectedMin);
        System.out.printf("  │ 写入吞吐量:    %,.0f msg/s            │%n", totalWrites / (elapsedMs / 1000.0));
        System.out.printf("  │ 全部完成:      %s                       │%n", finished ? "✓" : "✗ 超时!");
        System.out.println("  └─────────────────────────────────────────┘");

        Assertions.assertTrue(finished, "应在 60 秒内完成");
        Assertions.assertEquals(0, errors.get(), "不应有错误");
        Assertions.assertEquals(broadcastCount, broadcastDone.get(), "所有广播应完成");
        Assertions.assertEquals(privateMsgCount, privateDone.get(), "所有私聊应完成");
    }

    // ═══════════════════════════════════════════════════════════════
    // 测试 6: 内存占用评估
    // ═══════════════════════════════════════════════════════════════
    @Test
    @DisplayName("测试6: 内存占用评估 - 万人连接内存开销")
    void testMemoryFootprint() {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("测试6: 内存占用评估");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        Runtime rt = Runtime.getRuntime();
        rt.gc();
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        rt.gc();

        long totalMem = rt.totalMemory();
        long freeMem = rt.freeMemory();
        long usedMem = totalMem - freeMem;
        long maxMem = rt.maxMemory();

        double usedMB = usedMem / (1024.0 * 1024.0);
        double totalMB = totalMem / (1024.0 * 1024.0);
        double maxMB = maxMem / (1024.0 * 1024.0);
        double perPlayerKB = usedMem / 1024.0 / PLAYER_COUNT;

        System.out.println();
        System.out.println("  ┌─────────────────────────────────────────┐");
        System.out.printf("  │ 在线玩家数:    %,10d                  │%n", PLAYER_COUNT);
        System.out.printf("  │ 已用内存:      %10.1f MB             │%n", usedMB);
        System.out.printf("  │ 当前堆大小:    %10.1f MB             │%n", totalMB);
        System.out.printf("  │ 最大堆大小:    %10.1f MB             │%n", maxMB);
        System.out.printf("  │ 平均每玩家:    %10.1f KB             │%n", perPlayerKB);
        System.out.println("  └─────────────────────────────────────────┘");

        // 万人在线应该控制在 2GB 以内
        Assertions.assertTrue(usedMB < 2048, "内存使用应 < 2GB，实际 " + usedMB + " MB");
    }
}
