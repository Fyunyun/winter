package com.winter.core.db; // 声明包名：该类位于 com.winter.db 包下

import com.zaxxer.hikari.HikariDataSource; // 导入 HikariCP 数据源类，用于管理 MySQL 连接池
import redis.clients.jedis.Jedis; // 导入 Jedis 客户端类，用于操作 Redis 连接
import redis.clients.jedis.JedisPool; // 导入 Jedis 连接池类，用于复用 Redis 连接
import redis.clients.jedis.JedisPoolConfig; // 导入 Jedis 连接池配置类
import java.sql.Connection; // 导入 JDBC Connection 接口，代表数据库连接
import java.sql.SQLException; // 导入 SQLException，用于处理数据库相关异常



public class DbManager { // 定义数据库管理类 DbManager
    private static HikariDataSource ds = new HikariDataSource(); // 创建并持有一个静态的 Hikari 数据源（MySQL 连接池）
    private static JedisPool jedisPool = new JedisPool(new JedisPoolConfig(), "127.0.0.1", 6379); // 创建并持有一个静态的 Redis 连接池，地址 127.0.0.1:6379

    static { // 静态初始化块：类加载时执行，用于初始化数据源配置
        ds.setJdbcUrl("jdbc:mysql://localhost:3306/winter_game"); // 设置 MySQL JDBC 连接 URL（数据库 winter_game）
        ds.setUsername("root"); // 设置数据库用户名
        ds.setPassword("123456"); // 设置数据库密码
        ds.setMaximumPoolSize(10); // 设置连接池最大连接数为 10
    }

    public static Connection getConnection() throws SQLException { return ds.getConnection(); } // 获取一个 MySQL 连接（从连接池中借出），可能抛出 SQLException
    public static Jedis getJedis() { return jedisPool.getResource(); } // 获取一个 Redis 连接（从连接池中借出），调用方使用完需 close() 归还
}