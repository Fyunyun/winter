package com.winter.modules.building;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.alibaba.fastjson.JSON;
import com.winter.common.model.BuildingModel;
import com.winter.core.db.DbManager;

import redis.clients.jedis.Jedis;

public class BuildingDao {

    // 保存到redis
    public void saveBuildingToRedis(long pid, BuildingModel build) {
        try (Jedis redis = DbManager.getJedis()) {
            redis.hset("p:build:" + pid, String.valueOf(build.getBuildingType()), JSON.toJSONString(build));
        }
    }

    // 保存到MySQL
    public void saveBuildingToMysql(long pid, BuildingModel build) {
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
    
    // 从MySQL加载建筑数据
    public BuildingModel loadFromMysql(long pid, int type) {
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

}
