package com.winter.modules.battle.model;

/**
 * 对应配置表里的单个效果条目
 * 例如：{type: MOD_DAMAGE, param1: 50, param2: 0} 代表增加50%伤害
 */
public class BattleEffectConfig {
    private EffectType type; // 效果枚举类型
    private int param1; // 参数1 (通常是数值)
    private int param2; // 参数2 (通常是子类型或持续回合)

    public BattleEffectConfig(EffectType type, int param1, int param2) {
        this.type = type;
        this.param1 = param1;
        this.param2 = param2;
    }

    // Getters
    public EffectType getType() {
        return type;
    }

    public int getParam1() {
        return param1;
    }

    public int getParam2() {
        return param2;
    }
}