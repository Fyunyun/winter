package com.winter.core.util;

import java.util.concurrent.*;

/**
 * 全局业务线程池管理器
 * <p>
 * 将耗时的业务逻辑（聊天敏感词过滤、Redis 读写、广播遍历等）从 Netty I/O 线程剥离，
 * 避免阻塞 I/O 线程导致所有连接的读写延迟。
 * <p>
 * 架构：Netty I/O 线程 → 解码 + 鉴权 → 提交到业务线程池 → 业务处理 → write 回 Channel
 */
public class GameExecutor {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GameExecutor.class);

    /**
     * 通用业务线程池（处理聊天、建筑、采集、好友等模块逻辑）
     * 核心线程 8，最大线程 32，队列 8192 防止瞬时流量打爆内存
     */
    private static final ThreadPoolExecutor BUSINESS_POOL = new ThreadPoolExecutor(
            8,                          // 核心线程数
            32,                         // 最大线程数
            60L, TimeUnit.SECONDS,      // 空闲线程存活时间
            new LinkedBlockingQueue<>(8192),   // 有界队列，防止 OOM
            new ThreadFactory() {
                private int count = 0;

                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "biz-worker-" + (count++));
                    t.setDaemon(true);
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()  // 队列满时由调用者线程执行（降级保底）
    );

    /**
     * 提交业务任务到线程池
     *
     * @param task 业务逻辑
     */
    public static void execute(Runnable task) {
        BUSINESS_POOL.execute(task);
    }

    /**
     * 提交带返回值的业务任务
     */
    public static <T> Future<T> submit(Callable<T> task) {
        return BUSINESS_POOL.submit(task);
    }

    /**
     * 获取线程池状态（用于监控）
     */
    public static String getStatus() {
        return String.format("[业务线程池] 活跃=%d, 池大小=%d, 队列=%d, 已完成=%d",
                BUSINESS_POOL.getActiveCount(),
                BUSINESS_POOL.getPoolSize(),
                BUSINESS_POOL.getQueue().size(),
                BUSINESS_POOL.getCompletedTaskCount());
    }

    /**
     * 优雅关闭线程池
     */
    public static void shutdown() {
        logger.info("正在关闭业务线程池...");
        BUSINESS_POOL.shutdown();
        try {
            if (!BUSINESS_POOL.awaitTermination(10, TimeUnit.SECONDS)) {
                BUSINESS_POOL.shutdownNow();
            }
        } catch (InterruptedException e) {
            BUSINESS_POOL.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("业务线程池已关闭");
    }
}
