package com.winter.core.db;

import javax.sql.DataSource;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import java.sql.Connection;
import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 数据库管理类，通过 Spring 自动注入 DataSource，Redis 从配置文件读取。
 * 保留静态方法供全局调用。
 */
@Component
public class DbManager {

    private static DataSource dataSource;
    private static JedisPool jedisPool;

    public DbManager(DataSource ds,
                     @Value("${redis.host:127.0.0.1}") String redisHost,
                     @Value("${redis.port:6379}") int redisPort) {
        DbManager.dataSource = ds;
        DbManager.jedisPool = new JedisPool(new JedisPoolConfig(), redisHost, redisPort);
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static Jedis getJedis() {
        return jedisPool.getResource();
    }
}