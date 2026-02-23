package com.winter.modules.battle.config;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 单条技能触发条件配置
 * 
 * 示例：
 *   { "type": 1, "param": 30 }
 *   表示 SELF_HP_BELOW(30%) → 自身血量低于 30% 时满足
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConditionConfig {
    /** 条件类型 ID，对应 {@link ConditionType} */
    private int type;
    /** 条件参数（百分比/数值/buffId 等，视 type 而定） */
    private int param;
}
