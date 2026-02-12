package com.winter.modules.battle.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum BuffType {
    // --- 增益 (Positive) ---
    ATK_UP(1), // 攻击力提升 (value = 百分比/固定值)
    DEF_UP(2), // 防御力提升
    SPEED_UP(3), // 速度提升
    SHIELD(4), // 护盾 (抵挡伤害)

    // --- 减益 (Negative) ---
    ATK_DOWN(11), // 攻击力下降
    DEF_DOWN(12), // 防御力下降
    POISON(13), // 中毒 (回合开始扣血)
    STUN(14), // 眩晕 (跳过回合)
    BURN(15); // 燃烧 (回合结束扣血)

    private final int id;

}