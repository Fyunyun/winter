package com.winter.core.db;

import redis.clients.jedis.Jedis;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import com.winter.common.model.PlayerModel;

public class DataService {
    private static final String REDIS_KEY_PREFIX = "p:data:";
        
        // 2. 实时更新 Redis (比如砍树加木材)
    public static void updateResourceInRedis(PlayerModel model) {
        try (Jedis redis = DbManager.getJedis()) {
            String key = REDIS_KEY_PREFIX + model.getPlayerId();
            Map<String, String> map = new HashMap<>();
            map.put("wood", String.valueOf(model.getWood()));
            map.put("coal", String.valueOf(model.getCoal()));
            map.put("level", String.valueOf(model.getLevel()));
            map.put("x", String.valueOf(model.getX()));
            map.put("y", String.valueOf(model.getY()));
            if (model.getName() != null) {
                map.put("name", model.getName());
            }
            redis.hmset(key, map); // 使用 hmset，类型就是 hash
        }
    }

    // 3. 定时/离线落库 (MySQL)
    public static void flushToMysql(PlayerModel model) {
        String sql = "UPDATE player_main SET wood=?, coal=?, level=?, x=?, y=? WHERE id=?";
        try (Connection conn = DbManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, model.getWood());
            ps.setLong(2, model.getCoal());
            ps.setInt(3, model.getLevel());
            ps.setFloat(4, model.getX());
            ps.setFloat(5, model.getY());
            ps.setLong(6, model.getPlayerId());
            ps.executeUpdate();
            System.out.println("[MySQL] 玩家数据落库成功: " + model.getPlayerId());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}