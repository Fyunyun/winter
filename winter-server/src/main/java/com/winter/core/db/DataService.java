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
        
    // 注册新账号
    public static boolean register(String username, String password) {

        long newPlayerId = System.currentTimeMillis();

        String sqlAccount = "Insert INTO account (username, password, player_id) VALUES (?, ?, ?)";
        String sqlPlayer = "Insert INTO player_main (id, name, wood, coal, level, x, y) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String sqlBuilding = "Insert INTO player_building (player_id, building_type, level, status, finish_time) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DbManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 插入账号表
                PreparedStatement psAccount = conn.prepareStatement(sqlAccount);
                psAccount.setString(1, username);
                psAccount.setString(2, password);
                psAccount.setLong(3, newPlayerId);
                psAccount.executeUpdate();

                // 插入玩家主表
                PreparedStatement psPlayer = conn.prepareStatement(sqlPlayer);
                psPlayer.setLong(1, newPlayerId);
                psPlayer.setString(2, username); // 默认角色名为用户名
                psPlayer.setLong(3, 100); // 初始木材
                psPlayer.setLong(4, 100); // 初始煤炭
                psPlayer.setInt(5, 1); // 初始等级
                psPlayer.setFloat(6, 0f); // 初始X坐标
                psPlayer.setFloat(7, 0f); // 初始Y坐标
                psPlayer.executeUpdate();

                // 插入初始建筑数据 (熔炉, 兵营, 伐木场)
                PreparedStatement psBuilding = conn.prepareStatement(sqlBuilding);
                psBuilding.setLong(1, newPlayerId);
                psBuilding.setInt(2, 1); // 熔炉
                psBuilding.setInt(3, 1); // 等级1
                psBuilding.setInt(4, 0); // 闲置
                psBuilding.setLong(5, 0L); // 无结束时间
                psBuilding.executeUpdate();

                conn.commit();
                return true;

            } catch (Exception e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

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