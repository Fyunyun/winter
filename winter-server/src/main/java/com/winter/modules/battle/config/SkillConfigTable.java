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
 * 技能配置表 —— 全局单例，负责从 JSON 文件加载全部技能配置
 * 
 * 使用方式：
 *   // 1. 服务器启动时加载一次
 *   SkillConfigTable.getInstance().load("config/skill_config.json");
 *   
 *   // 2. 运行时查询
 *   SkillConfig cfg = SkillConfigTable.getInstance().get(1001);
 */
@Slf4j
public class SkillConfigTable {

    private static final SkillConfigTable INSTANCE = new SkillConfigTable();

    /** skillId → SkillConfig */
    private final Map<Integer, SkillConfig> table = new LinkedHashMap<>();

    private SkillConfigTable() {}

    public static SkillConfigTable getInstance() {
        return INSTANCE;
    }

    // ======================== 加载 ========================

    /**
     * 从 classpath 资源加载配置（推荐）
     * @param resourcePath classpath 下的路径，例如 "config/skill_config.json"
     */
    /**
     * 加载技能配置文件
     * <p>
     * 从指定的资源路径读取JSON格式的技能配置文件，解析并存储到内存中。
     * 如果文件不存在或加载失败，会打印相应的错误信息。
     * </p>
     *
     * @param resourcePath 技能配置文件的资源路径（相对于类路径）
     *                     例如: "config/skills.json"
     */
    public void load(String resourcePath) {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.out.println("[SkillConfigTable] 配置文件未找到: " + resourcePath);
                return;
            }
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            parseAndStore(json);
            System.out.println("[SkillConfigTable] 加载完成, 共 " + table.size() + " 个技能配置");
        } catch (IOException e) {
            System.out.println("[SkillConfigTable] 加载配置失败: " + resourcePath + ", 错误: " + e.getMessage());
        }
    }

    /**
     * 直接从 JSON 字符串加载（方便测试）
     */
    public void loadFromString(String json) {
        parseAndStore(json);
    }

    private void parseAndStore(String json) {
        List<SkillConfig> list = JSON.parseArray(json, SkillConfig.class);
        if (list == null || list.isEmpty()) {
            log.warn("[SkillConfigTable] 配置为空");
            return;
        }
        table.clear();
        for (SkillConfig cfg : list) {
            if (table.containsKey(cfg.getSkillId())) {
                log.warn("[SkillConfigTable] 重复的 skillId: {}, 后者覆盖前者", cfg.getSkillId());
            }
            table.put(cfg.getSkillId(), cfg);
        }
    }

    // ======================== 查询 ========================

    /**
     * 根据 skillId 获取配置
     * @return 配置对象，不存在时返回 null
     */
    public SkillConfig get(int skillId) {
        return table.get(skillId);
    }

    /**
     * 获取所有配置（只读视图）
     */
    public Map<Integer, SkillConfig> getAll() {
        return Collections.unmodifiableMap(table);
    }

    /**
     * 配置表是否为空
     */
    public boolean isEmpty() {
        return table.isEmpty();
    }

    /**
     * 配置数量
     */
    public int size() {
        return table.size();
    }
}
