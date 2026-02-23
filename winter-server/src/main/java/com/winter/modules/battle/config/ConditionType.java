package com.winter.modules.battle.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 技能触发条件类型枚举
 * 配置表中的 conditionType 字段对应此枚举
 */
@AllArgsConstructor
@Getter
public enum ConditionType {
    NONE(0),                  // 无条件（始终满足）
    SELF_HP_BELOW(1),         // 自身血量低于 X% 时触发
    SELF_HP_ABOVE(2),         // 自身血量高于 X% 时触发
    TARGET_HP_BELOW(3),       // 目标血量低于 X% 时触发
    TARGET_HP_ABOVE(4),       // 目标血量高于 X% 时触发
    ROUND_GE(5),              // 当前回合 >= X 时触发
    ROUND_LE(6),              // 当前回合 <= X 时触发
    TARGET_HAS_BUFF(7),       // 目标拥有指定 BuffType(id=X) 时触发
    SELF_HAS_BUFF(8),         // 自身拥有指定 BuffType(id=X) 时触发
    TARGET_TYPE_IS(9),        // 目标兵种类型为 X 时触发
    SELF_BUFF_COUNT_GE(10);   // 自身 Buff 数量 >= X 时触发

    private final int id;

    public static ConditionType fromId(int id) {
        for (ConditionType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        return null;
    }
}
