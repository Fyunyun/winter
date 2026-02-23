package com.winter.modules.battle.config;

import com.winter.modules.battle.core.ConditionChecker;
import com.winter.modules.battle.model.BattleEffectConfig;
import com.winter.modules.battle.model.EffectType;
import com.winter.modules.battle.model.skill.GeneralSkill;
import com.winter.modules.battle.model.skill.SkillTrigger;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 技能工厂 —— 根据 SkillConfig 创建 GeneralSkill 运行时实例
 * 
 * 使用方式：
 *   SkillConfig cfg = SkillConfigTable.getInstance().get(1001);
 *   GeneralSkill skill = SkillFactory.create(cfg);
 */
@Slf4j
public class SkillFactory {

    private static final SkillTrigger[] TRIGGERS = SkillTrigger.values();
    private static final EffectType[] EFFECT_TYPES = EffectType.values();

    /**
     * 根据配置创建技能实例
     */
    public static GeneralSkill create(SkillConfig cfg) {
        if (cfg == null) {
            return null;
        }

        // 1. 解析触发时机
        SkillTrigger trigger = parseTrigger(cfg.getTrigger());

        // 2. 解析条件列表
        List<ConditionChecker> conditions = parseConditions(cfg.getConditions());

        // 3. 解析效果列表
        List<BattleEffectConfig> effects = parseEffects(cfg.getEffects());

        return new GeneralSkill(
                cfg.getSkillId(),
                trigger,
                cfg.getProbability(),
                cfg.getCooldown(),
                conditions,
                effects
        );
    }

    /**
     * 批量创建（根据 skillId 列表）
     */
    public static List<GeneralSkill> createAll(List<Integer> skillIds) {
        if (skillIds == null || skillIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<GeneralSkill> result = new ArrayList<>(skillIds.size());
        for (int id : skillIds) {
            SkillConfig cfg = SkillConfigTable.getInstance().get(id);
            if (cfg == null) {
                log.warn("[SkillFactory] 找不到技能配置: skillId={}", id);
                continue;
            }
            result.add(create(cfg));
        }
        return result;
    }

    // ======================== 内部解析 ========================

    private static SkillTrigger parseTrigger(int triggerOrdinal) {
        if (triggerOrdinal < 0 || triggerOrdinal >= TRIGGERS.length) {
            log.warn("[SkillFactory] 无效的 trigger: {}, 默认使用 ROUND_START", triggerOrdinal);
            return SkillTrigger.ROUND_START;
        }
        return TRIGGERS[triggerOrdinal];
    }

    private static List<ConditionChecker> parseConditions(List<ConditionConfig> configs) {
        if (configs == null || configs.isEmpty()) {
            return Collections.emptyList();
        }
        List<ConditionChecker> checkers = new ArrayList<>(configs.size());
        for (ConditionConfig cc : configs) {
            ConditionType ct = ConditionType.fromId(cc.getType());
            if (ct == null) {
                log.warn("[SkillFactory] 未知条件类型: {}", cc.getType());
                continue;
            }
            checkers.add(new GeneralConditionChecker(ct, cc.getParam()));
        }
        return checkers;
    }

    private static List<BattleEffectConfig> parseEffects(List<EffectConfig> configs) {
        if (configs == null || configs.isEmpty()) {
            return Collections.emptyList();
        }
        List<BattleEffectConfig> effects = new ArrayList<>(configs.size());
        for (EffectConfig ec : configs) {
            EffectType et = findEffectType(ec.getType());
            if (et == null) {
                log.warn("[SkillFactory] 未知效果类型: {}", ec.getType());
                continue;
            }
            effects.add(new BattleEffectConfig(et, ec.getParam1(), ec.getParam2()));
        }
        return effects;
    }

    private static EffectType findEffectType(int id) {
        return EffectType.fromId(id);
    }
}
