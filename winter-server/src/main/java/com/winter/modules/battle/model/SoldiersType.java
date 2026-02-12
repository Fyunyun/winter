package com.winter.modules.battle.model;
import lombok.AllArgsConstructor;

// SoldiersType.java
@AllArgsConstructor
public enum SoldiersType {
    INFANTRY(1), // 步兵
    CAVALRY(2),  // 骑兵
    ARCHER(3);   // 弓兵

    private final int value;

    public int getValue() {
        return value;
    }
    // 获取克制倍率
    public double getCounterMod(SoldiersType target) {
        if (this == INFANTRY && target == CAVALRY) return 1.5; // 步克骑 (枪兵)
        if (this == CAVALRY && target == ARCHER) return 1.5;   // 骑克弓
        if (this == ARCHER && target == INFANTRY) return 1.5;  // 弓克步
        return 1.0;
    }
}