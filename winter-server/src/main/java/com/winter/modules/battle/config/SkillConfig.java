package com.winter.modules.battle.config;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 技能配置表 —— 单条技能的完整配置
 * 
 * 对应 JSON 中的一个元素，包含技能的所有静态属性。
 * 运行时会被 {@link SkillConfigTable} 加载并缓存，由 {@link SkillFactory} 转换为 GeneralSkill 实例。
 * 
 * 字段说明：
 * - skillId:      技能唯一 ID (主键)
 * - name:         技能名称 (用于调试/日志/前端显示)
 * - description:  技能描述文字
 * - trigger:      触发时机，对应 {@link com.winter.modules.battle.model.skill.SkillTrigger} 的 ordinal()
 *                 0=BATTLE_START, 1=ROUND_START, 2=BEFORE_ATTACK, 3=AFTER_ATTACK,
 *                 4=ON_DAMAGED, 5=ON_DEATH, 6=ROUND_END
 * - probability:  触发概率 (0.0 ~ 1.0, 1.0 = 必定触发)
 * - cooldown:     冷却回合数 (0 = 无冷却)
 * - conditions:   额外触发条件列表 (全部满足才触发，为空则无额外条件)
 * - effects:      技能效果列表 (按顺序执行)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SkillConfig {

    private int skillId;
    private String name;
    private String description;

    /** 触发时机 (SkillTrigger.ordinal()) */
    private int trigger;

    /** 触发概率 (0.0 ~ 1.0) */
    private double probability;

    /** 冷却回合数 */
    private int cooldown;

    /** 触发条件列表（全部 AND） */
    private List<ConditionConfig> conditions;

    /** 效果列表（按顺序执行） */
    private List<EffectConfig> effects;
}
