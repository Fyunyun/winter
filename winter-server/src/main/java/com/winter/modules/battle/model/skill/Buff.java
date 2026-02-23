package com.winter.modules.battle.model.skill;

import com.winter.modules.battle.config.BuffCategory;
import com.winter.modules.battle.config.BuffConfig;
import com.winter.modules.battle.config.BuffConfigTable;
import lombok.Getter;
import lombok.Setter;

/**
 * Buff 运行时实例
 *
 * buffId 对应 buff_config.json 中的配置，通过 {@link BuffConfigTable} 获取元数据。
 */
@Getter
@Setter
public class Buff {
    private int buffId;       // 对应 BuffConfig.buffId
    private int value;        // 数值 (例如: 10 代表提升 10%)
    private int rounds;       // 剩余回合数 (持续时间)
    private long sourceId;    // 施法者ID (用于统计伤害来源)

    public Buff(int buffId, int value, int rounds, long sourceId) {
        this.buffId = buffId;
        this.value = value;
        this.rounds = rounds;
        this.sourceId = sourceId;
    }

    /**
     * 获取此 Buff 对应的配置
     */
    public BuffConfig getConfig() {
        return BuffConfigTable.getInstance().get(buffId);
    }

    /**
     * 获取行为分类
     */
    public BuffCategory getCategory() {
        BuffConfig cfg = getConfig();
        return cfg != null ? cfg.getBuffCategory() : null;
    }

    /**
     * 是否是护盾类
     */
    public boolean isShield() {
        BuffConfig cfg = getConfig();
        return cfg != null && cfg.isShield();
    }

    /**
     * 是否是控制类（眩晕等）
     */
    public boolean isControl() {
        BuffConfig cfg = getConfig();
        return cfg != null && cfg.isControl();
    }

    /**
     * 是否是 DOT 类（中毒、燃烧等）
     */
    public boolean isDot() {
        BuffConfig cfg = getConfig();
        return cfg != null && cfg.isDot();
    }

    /**
     * 是否是属性修改类
     */
    public boolean isAttrMod() {
        BuffConfig cfg = getConfig();
        return cfg != null && cfg.isAttrMod();
    }

    /**
     * 是否是增益
     */
    public boolean isPositive() {
        BuffConfig cfg = getConfig();
        return cfg != null && cfg.isPositive();
    }

    // 减少回合数
    public void tick() {
        if (this.rounds > 0) {
            this.rounds--;
        }
    }

    // 判断Buff是否已过期
    public boolean isExpired() {
        return this.rounds <= 0;
    }
}