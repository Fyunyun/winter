package com.winter.service;

import com.alibaba.fastjson.JSON;
import com.winter.db.DbManager;
import com.winter.model.BuildingModel;
import com.winter.model.PlayerModel;
import redis.clients.jedis.Jedis;
import com.winter.db.DataService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;


public class BuildingService {

    // --- 1. 获取某个建筑 (先查 Redis，没有再查 MySQL) ---
    public static BuildingModel getBuilding(long playerId, int type) {
        String key = "p:build:" + playerId;
        try (Jedis redis = DbManager.getJedis()) {
            // A. 查 Redis
            String json = redis.hget(key, String.valueOf(type));
            if (json != null) {
                return JSON.parseObject(json, BuildingModel.class);
            }
            
            // B. Redis 没有，查 MySQL 并回填
            BuildingModel dbModel = loadFromMysql(playerId, type);
            if (dbModel != null) {
                redis.hset(key, String.valueOf(type), JSON.toJSONString(dbModel));
            } else {
                // 如果数据库也没有，说明玩家还没建这个建筑，创建一个默认的 0 级建筑
                dbModel = new BuildingModel(type, 0); 
                redis.hset(key, String.valueOf(type), JSON.toJSONString(dbModel));
            }
            return dbModel;
        }
    }

    // --- 2. 升级建筑 (核心事务逻辑) ---
    public static String upgradeBuilding(PlayerModel player, int type) {
        // A. 获取建筑数据
        BuildingModel build = getBuilding(player.getPlayerId(), type);
        
        // B. 各种检查
        if (build.isUpgrading()) {
            return "错误：建筑正在升级中，请稍后再试";
        }
        
        // C. 检查资源 (假设升级需要：等级 * 100 木头)
        long costWood = (build.getLevel() + 1) * 100L;
        if (player.getWood() < costWood) {
            return "错误：木材不足！需要 " + costWood;
        }

        // D. 【执行扣费】 (更新 Redis 中的玩家资源)
        player.setWood(player.getWood() - costWood);
        DataService.updateResourceInRedis(player); // 之前写的更新资源方法

        // E. 【执行升级】 (更新 Redis 中的建筑状态)
        build.setStatus(1); // 标记为升级中
        // 假设升级耗时 60秒 (实际配置应从策划表读取)
        long durationMs = 60 * 1000L; 
        build.setFinishTime(System.currentTimeMillis() + durationMs);

        // F. 保存建筑数据到 Redis
        saveBuildingToRedis(player.getPlayerId(), build);
        
        // G. 异步/同步保存到 MySQL (防止回档)
        // 在实际项目中，这里通常是丢给 Log 线程或者定时任务
        saveBuildingToMysql(player.getPlayerId(), build);

        return "成功：建筑开始升级！消耗木材 " + costWood + "，将在60秒后完成。";
    }

    // --- 辅助方法 ---

    private static void saveBuildingToRedis(long pid, BuildingModel build) {
        try (Jedis redis = DbManager.getJedis()) {
            redis.hset("p:build:" + pid, String.valueOf(build.getBuildingType()), JSON.toJSONString(build));
        }
    }

    private static BuildingModel loadFromMysql(long pid, int type) {
        String sql = "SELECT level, status, finish_time FROM player_building WHERE player_id=? AND building_type=?";
        try (Connection conn = DbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, pid);
            ps.setInt(2, type);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                BuildingModel m = new BuildingModel(type, rs.getInt("level"));
                m.setStatus(rs.getInt("status"));
                m.setFinishTime(rs.getLong("finish_time"));
                return m;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    private static void saveBuildingToMysql(long pid, BuildingModel build) {
        // 使用 ON DUPLICATE KEY UPDATE 语法，不存在则插入，存在则更新
        String sql = "INSERT INTO player_building (player_id, building_type, level, status, finish_time) " +
                     "VALUES (?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE level=?, status=?, finish_time=?";
        try (Connection conn = DbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            // Insert 部分
            ps.setLong(1, pid); ps.setInt(2, build.getBuildingType());
            ps.setInt(3, build.getLevel()); ps.setInt(4, build.getStatus());
            ps.setLong(5, build.getFinishTime());
            // Update 部分
            ps.setInt(6, build.getLevel()); ps.setInt(7, build.getStatus());
            ps.setLong(8, build.getFinishTime());
            
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }
}