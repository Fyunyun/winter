package com.winter.modules.battle.model;

public enum SkillTrigger {
    BATTLE_START, // 战斗开始时 (如：全军攻击+10%)
    ROUND_START, // 回合开始时 (如：每回合回血)
    BEFORE_ATTACK, // 攻击前 (如：本次攻击必暴击)
    AFTER_ATTACK, // 攻击后 (如：吸血)
    ON_DAMAGED, // 被打时 (如：反伤)
    ON_DEATH, // 死亡时 (如：自爆)
    ROUND_END // 回合结束时 (如：持续伤害)
}