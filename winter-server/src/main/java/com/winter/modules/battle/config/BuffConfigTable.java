package com.winter.modules.battle.config;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Buff 配置表 —— 全局单例，负责从 JSON 文件加载全部 Buff 配置
 *
 * 使用方式：
 *   // 1. 服务器启动时加载一次
 *   BuffConfigTable.getInstance().load("config/buff_config.json");
 *
 *   // 2. 运行时查询
 *   BuffConfig cfg = BuffConfigTable.getInstance().get(1);
 */
@Slf4j
public class BuffConfigTable {

    private static final BuffConfigTable INSTANCE = new BuffConfigTable();

    /** buffId → BuffConfig */
    private final Map<Integer, BuffConfig> table = new LinkedHashMap<>();

    private BuffConfigTable() {}

    public static BuffConfigTable getInstance() {
        return INSTANCE;
    }

    // ======================== 加载 ========================

    /**
     * 从 classpath 资源加载配置
     * @param resourcePath classpath 下的路径，例如 "config/buff_config.json"
     */
    public void load(String resourcePath) {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                log.error("[BuffConfigTable] 配置文件未找到: {}", resourcePath);
                return;
            }
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            parseAndStore(json);
            log.info("[BuffConfigTable] 加载完成, 共 {} 个 Buff 配置", table.size());
        } catch (IOException e) {
            log.error("[BuffConfigTable] 加载配置失败: {}", resourcePath, e);
        }
    }

    /**
     * 直接从 JSON 字符串加载（方便测试）
     */
    public void loadFromString(String json) {
        parseAndStore(json);
    }

    private void parseAndStore(String json) {
        List<BuffConfig> list = JSON.parseArray(json, BuffConfig.class);
        if (list == null || list.isEmpty()) {
            log.warn("[BuffConfigTable] 配置为空");
            return;
        }
        table.clear();
        for (BuffConfig cfg : list) {
            if (table.containsKey(cfg.getBuffId())) {
                log.warn("[BuffConfigTable] 重复的 buffId: {}, 后者覆盖前者", cfg.getBuffId());
            }
            table.put(cfg.getBuffId(), cfg);
        }
    }

    // ======================== 查询 ========================

    /**
     * 根据 buffId 获取配置
     */
    public BuffConfig get(int buffId) {
        return table.get(buffId);
    }

    /**
     * 获取全部配置（不可修改视图）
     */
    public Map<Integer, BuffConfig> getAll() {
        return Collections.unmodifiableMap(table);
    }

    /**
     * 配置总数
     */
    public int size() {
        return table.size();
    }
}
