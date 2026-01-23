package com.winter;

import com.winter.common.model.PlayerModel;
import com.winter.core.db.DataService;
import com.winter.core.db.DbManager;

import redis.clients.jedis.Jedis;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class DatabaseTest {

    // 使用一个特殊的测试 ID，避免和正常玩家冲突
    private static final long TEST_PLAYER_ID = 99999; 

    public static void main(String[] args) {
        System.out.println("========== 开始数据库集成测试 ==========");

        try {
            // 1. 清理环境 (删除旧的测试数据，保证每次运行环境干净)
            cleanUp();

            // 2. 模拟：新玩家注册 (直接写入 MySQL 模拟初始号)
            System.out.println("\n[步骤 1] 模拟创建新账号...");
            createInitialData();

            // 3. 模拟：玩家登录 (测试 Load 逻辑)
            System.out.println("\n[步骤 2] 模拟玩家登录 (加载数据)...");
            PlayerModel player = DataService.loadPlayer(TEST_PLAYER_ID);
            
            if (player != null) {
                System.out.println("SUCCESS: 玩家数据加载成功!");
                System.out.println(" -> 当前木材: " + player.getWood());
                System.out.println(" -> 当前等级: " + player.getLevel());
            } else {
                System.err.println("FAILED: 无法加载玩家数据！");
                return;
            }

            // 4. 模拟：Redis 缓存验证
            System.out.println("\n[步骤 3] 验证 Redis 缓存...");
            try (Jedis jedis = DbManager.getJedis()) {
                String redisKey = "p:data:" + TEST_PLAYER_ID; // 注意这里要和你 DataService 里的 Key 前缀一致
                if (jedis.exists(redisKey)) {
                    System.out.println("SUCCESS: 数据已自动缓存到 Redis!");
                } else {
                    System.err.println("FAILED: Redis 中没有数据，缓存逻辑未生效！");
                }
            }

            // 5. 模拟：游戏逻辑 (修改内存 + 更新 Redis)
            System.out.println("\n[步骤 4] 模拟游戏操作 (生火消耗木材)...");
            // 假设初始 1000，消耗 100，剩余 900
            player.setWood(player.getWood() - 100); 
            // 同步到 Redis
            DataService.updateResourceInRedis(player);
            System.out.println(" -> 玩家内存木材变为: " + player.getWood());

            // 6. 模拟：定时存盘 (落库 MySQL)
            System.out.println("\n[步骤 5] 模拟定时存盘 (写入 MySQL)...");
            DataService.flushToMysql(player);
            System.out.println(" -> 执行 flushToMysql 完成");

            // 7. 验证：直接查 MySQL 看看变了没
            System.out.println("\n[步骤 6] 最终验证 MySQL 数据...");
            checkMysqlDirectly();

        } catch (Exception e) {
            e.printStackTrace();
        }
        
        System.out.println("\n========== 测试结束 ==========");
    }

    // --- 辅助工具方法 ---

    // 清理 MySQL 和 Redis 中的测试数据
    private static void cleanUp() {
        System.out.println(" -> 清理旧数据...");
        // 清 MySQL
        try (Connection conn = DbManager.getConnection()) {
            conn.createStatement().execute("DELETE FROM player_main WHERE id = " + TEST_PLAYER_ID);
        } catch (Exception e) { e.printStackTrace(); }
        
        // 清 Redis
        try (Jedis jedis = DbManager.getJedis()) {
            jedis.del("p:data:" + TEST_PLAYER_ID);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // 在 MySQL 插入一条初始数据
    private static void createInitialData() {
        String sql = "INSERT INTO player_main (id, name, level, wood, coal) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, TEST_PLAYER_ID);
            ps.setString(2, "TestUser");
            ps.setInt(3, 1);
            ps.setLong(4, 1000); // 初始 1000 木头
            ps.setLong(5, 500);
            ps.executeUpdate();
            System.out.println(" -> 初始数据插入成功 (ID: " + TEST_PLAYER_ID + ", Wood: 1000)");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 绕过业务层，直接查库验证
    private static void checkMysqlDirectly() {
        try (Connection conn = DbManager.getConnection()) {
            var rs = conn.createStatement().executeQuery("SELECT wood FROM player_main WHERE id = " + TEST_PLAYER_ID);
            if (rs.next()) {
                long dbWood = rs.getLong("wood");
                System.out.println(" -> 数据库中的实际木材: " + dbWood);
                if (dbWood == 900) {
                    System.out.println("SUCCESS: 测试通过！数据流转完美闭环！");
                } else {
                    System.err.println("FAILED: 数据不一致，期望 900，实际 " + dbWood);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}