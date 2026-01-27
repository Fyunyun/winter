package com.winter.modules.auth;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.winter.common.model.PlayerModel;
import com.winter.core.db.DbManager;

import redis.clients.jedis.Jedis;

public class LoginDao {
    private static final String REDIS_KEY_PREFIX = "p:data:";

    // 登录验证
    public long LOGIN_VERIFY(String username, String password) {
        String sql = "SELECT password, player_id FROM account WHERE username=?";
        try (Connection conn = DbManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String storedPassword = rs.getString("password");
                if (storedPassword.equals(password)) {
                    return rs.getLong("player_id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1L;
    }

    // 登录加载
    public PlayerModel loadPlayer(long pid) {
        try (Jedis redis = DbManager.getJedis()) {
            String key = REDIS_KEY_PREFIX + pid;
            // 尝试从 Redis 获取
            if (redis.exists(key)) {
                Map<String, String> data = redis.hgetAll(key);
                if (data != null && !data.isEmpty()) {
                    PlayerModel model = new PlayerModel(pid);
                    model.setWood(parseLong(data.get("wood")));
                    model.setCoal(parseLong(data.get("coal")));
                    model.setLevel(parseInt(data.get("level")));
                    model.setName(data.get("name"));
                    model.setX(parseFloat(data.get("x")));
                    model.setY(parseFloat(data.get("y")));
                    model.setDirty(false);
                    return model;
                }
            }
            // Redis 没有，从 MySQL 读
            PlayerModel model = loadPlayerFromMysql(pid);
            if (model != null) {
                // 1. 准备要存入 Hash 的数据
                Map<String, String> hashData = new HashMap<>();
                hashData.put("wood", String.valueOf(model.getWood()));
                hashData.put("coal", String.valueOf(model.getCoal()));
                hashData.put("level", String.valueOf(model.getLevel()));
                if (model.getName() != null) {
                    hashData.put("name", model.getName());
                }
                hashData.put("x", String.valueOf(model.getX()));
                hashData.put("y", String.valueOf(model.getY()));

                // 2. 使用 hset 将 Map 存入 Redis（这会自动创建为 hash 类型）
                redis.hset(key, hashData);

                // 3. 设置过期时间（注意：hash 类型设置过期时间依然用 expire）
                redis.expire(key, 3600);
            }
            return model;
        }      
    }
       // Redis 没有命中时，从 MySQL 加载玩家主数据
    /**
     * 从 MySQL 的 {@code player_main} 表中按玩家 ID 加载玩家基础数据并构建 {@link PlayerModel}。
     * <p>
     * 查询字段包含：{@code id, name, wood, coal, level, x, y}。当查询无结果时返回 {@code null}。
     * 读取成功后会将模型标记为非脏数据（{@code dirty=false}），表示当前数据与数据库一致。
     * </p>
     *
     * @param pid 玩家唯一 ID
     * @return 读取到的 {@link PlayerModel}；若不存在该玩家或发生 {@link SQLException} 则返回
     *         {@code null}
     */
    private  PlayerModel loadPlayerFromMysql(long pid) { // 从 MySQL 加载指定 pid 的玩家数据并返回 PlayerModel
        String sql = "SELECT id, name, wood, coal, level, x, y FROM player_main WHERE id=?"; // 定义查询 SQL：按 id 查询玩家基础字段
        try (Connection conn = DbManager.getConnection(); // 获取数据库连接（try-with-resources 自动关闭）
                PreparedStatement ps = conn.prepareStatement(sql)) { // 预编译 SQL，防止注入并提升性能

            ps.setLong(1, pid); // 将第 1 个占位符 ? 绑定为玩家 id

            try (ResultSet rs = ps.executeQuery()) { // 执行查询并获取结果集（自动关闭）
                if (!rs.next())
                    return null; // 如果没有查询到记录，返回 null

                PlayerModel model = new PlayerModel(pid); // 创建玩家数据模型对象
                model.setName(rs.getString("name")); // 从结果集中读取 name 并设置到模型
                model.setWood(rs.getLong("wood")); // 从结果集中读取 wood 并设置到模型
                model.setCoal(rs.getLong("coal")); // 从结果集中读取 coal 并设置到模型
                model.setLevel(rs.getInt("level")); // 从结果集中读取 level 并设置到模型
                model.setX(rs.getFloat("x")); // 从结果集中读取 x 坐标并设置到模型
                model.setY(rs.getFloat("y")); // 从结果集中读取 y 坐标并设置到模型
                model.setDirty(false); // 标记为非脏数据：与数据库一致
                return model; // 返回加载完成的模型
            } // 结束 ResultSet 作用域并自动关闭

        } catch (SQLException e) { // 捕获 SQL 异常
            e.printStackTrace(); // 打印异常堆栈，便于排查问题
            return null; // 发生异常时返回 null
        } // 结束异常处理
    } // 结束方法

    private long parseLong(String value) {
        if (value == null || value.isEmpty())
            return 0L;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private int parseInt(String value) {
        if (value == null || value.isEmpty())
            return 0;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private float parseFloat(String value) {
        if (value == null || value.isEmpty())
            return 0f;
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return 0f;
        }
    }
}
