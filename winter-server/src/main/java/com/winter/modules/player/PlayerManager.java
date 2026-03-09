package com.winter.modules.player;

import com.google.protobuf.GeneratedMessageV3;
import com.winter.common.model.PlayerModel;
import com.winter.msg.MsgId.CmdId;

import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.winter.msg.PacketMsg.GamePacket;

public class PlayerManager {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PlayerManager.class);

    // 1. 维护 ID -> Channel 的映射
    private static final Map<Long, Channel> onlinePlayers = new ConcurrentHashMap<>();

    // 登录成功时调用
    public static void addPlayer(PlayerModel player, Channel channel) {
        onlinePlayers.put(player.getPlayerId(), channel);
        logger.info("[玩家管理] 玩家上线，当前在线人数: {}", onlinePlayers.size());
    }

    // 掉线时调用
    public static void removePlayer(long playerId) {
        onlinePlayers.remove(playerId);
        logger.info("[玩家管理] 玩家下线，当前在线人数: {}", onlinePlayers.size());
    }

    // 【核心】给指定玩家发包
    public static void sendToPlayer(long playerId, GeneratedMessageV3 message, CmdId cmd) {
        Channel channel = onlinePlayers.get(playerId);
        if (channel != null && channel.isActive()) {
            GamePacket packet = GamePacket.newBuilder()
                    .setCmd(cmd)
                    .setContent(message.toByteString())
                    .build();
            // 确保写操作在 Channel 所属 EventLoop 线程执行，避免并发竞争
            channel.writeAndFlush(packet);
            logger.debug("推送消息给 {}: {}", playerId, cmd);
        }
    }

    // 查询玩家是否在线
    public static boolean isOnline(long playerId) {
        return onlinePlayers.containsKey(playerId);
    }

    /**
     * 获取当前在线人数
     */
    public static int getOnlineCount() {
        return onlinePlayers.size();
    }

    /**
     * 高性能广播 —— 按 EventLoop 分组，先 write 再统一 flush
     * <p>
     * 优化点：
     * 1. 消息只序列化 1 次，所有 Channel 共享同一个 GamePacket 对象
     * 2. 按 EventLoop 分组，每组内的 write 操作在同一线程执行，无锁竞争
     * 3. 先 write（写入缓冲区）再统一 flush（刷出），将 N 次 syscall 减少为 1 次
     * 4. 通过 EventLoop.execute() 提交，不阻塞调用者线程
     */
    public static void broadcast(GeneratedMessageV3 message, CmdId cmd) {
        // 1. 消息只序列化一次
        GamePacket packet = GamePacket.newBuilder()
                .setCmd(cmd)
                .setContent(message.toByteString())
                .build();

        // 2. 按 EventLoop 分组，避免跨线程竞争
        Map<EventLoop, List<Channel>> grouped = new HashMap<>();
        for (Channel channel : onlinePlayers.values()) {
            if (channel.isActive()) {
                grouped.computeIfAbsent(channel.eventLoop(), k -> new ArrayList<>()).add(channel);
            }
        }

        // 3. 每组内：先 write 再统一 flush（大幅减少系统调用次数）
        for (Map.Entry<EventLoop, List<Channel>> entry : grouped.entrySet()) {
            EventLoop eventLoop = entry.getKey();
            List<Channel> channels = entry.getValue();

            eventLoop.execute(() -> {
                for (Channel ch : channels) {
                    ch.write(packet);       // 只写入出站缓冲区，不触发 flush
                }
                for (Channel ch : channels) {
                    ch.flush();             // 统一 flush，一次 syscall 刷出全部
                }
            });
        }

        logger.info("广播消息: {} 给 {} 个玩家 (分 {} 组)", cmd, onlinePlayers.size(), grouped.size());
    }
}
