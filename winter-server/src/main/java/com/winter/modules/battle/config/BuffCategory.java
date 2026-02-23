package com.winter.modules.battle.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Buff 行为分类 —— 决定 Buff 的结算逻辑
 *
 * 与 BuffConfig 配合使用，每个 Buff 配置指定一个 category，
 * 引擎根据 category 执行对应的通用逻辑，无需为每个 Buff 写 switch。
 */
@AllArgsConstructor
@Getter
public enum BuffCategory {

    /** 属性修改类：攻击/防御/速度 等提升或降低 */
    ATTR_MOD("attr_mod"),

    /** 持续伤害类：中毒、燃烧等 */
    DOT("dot"),

    /** 护盾类：吸收伤害 */
    SHIELD("shield"),

    /** 控制类：眩晕、沉默等 */
    CONTROL("control"),

    /** 持续恢复类：再生等 */
    HOT("hot");

    private final String code;

    public static BuffCategory fromCode(String code) {
        if (code == null) return null;
        for (BuffCategory c : values()) {
            if (c.code.equalsIgnoreCase(code)) {
                return c;
            }
        }
        return null;
    }
}
