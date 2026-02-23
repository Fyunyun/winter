package com.winter.modules.battle.model;

public enum EffectType {
    MOD_DAMAGE(1),      // 修正伤害倍率 (修改 Context)
    MOD_CRIT_RATE(2),   // 修正暴击率 (修改 Context)
    ADD_BUFF(3),        // 施加状态 (修改 Unit)
    HEAL(4),            // 立即回血 (修改 Unit)
    VAMPIRE(5),         // 吸血效果 (综合修改)
    REDUCE_CD(6);       // 减少技能冷却

    private int id;
    EffectType(int id) { this.id = id; }

    public int getId() { return id; }

    public static EffectType fromId(int id) {
        for (EffectType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        return null;
    }
}
