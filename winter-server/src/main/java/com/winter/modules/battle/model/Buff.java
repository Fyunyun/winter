package com.winter.modules.battle.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Buff {
    private int configId;     // 配置表ID (用于读取图标、描述)
    private BuffType type;    // 类型
    private int value;        // 数值 (例如: 10 代表提升 10%)
    private int rounds;       // 剩余回合数 (持续时间)
    private long sourceId;    // 施法者ID (用于统计伤害来源)

    public Buff(int configId, BuffType type, int value, int rounds, long sourceId) {
        this.configId = configId;
        this.type = type;
        this.value = value;
        this.rounds = rounds;
        this.sourceId = sourceId;
    }

    // 减少回合数
    public void tick() {
        if (this.rounds > 0) {
            this.rounds--;
        }
    }

    public boolean isExpired() {
        return this.rounds <= 0;
    }
}