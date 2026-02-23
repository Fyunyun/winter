package com.winter.modules.battle.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import java.util.Iterator;
import com.winter.modules.battle.core.BattleContext;
import com.winter.modules.battle.config.BuffConfig;
import com.winter.modules.battle.model.skill.Buff;
import com.winter.modules.battle.model.skill.GeneralSkill;
import com.winter.modules.battle.model.skill.SkillTrigger;

// BattleUnit.java
/**
 * 战斗单位模型类
 * 
 * 代表战场上的一个战斗单位（士兵或英雄），包含基础属性、动态属性、Buff系统和技能系统。
 * 
 * 主要功能：
 * 1. 管理单位的生命值、攻防等基础属性
 * 2. 动态计算受Buff影响的攻防值
 * 3. 维护Buff列表并处理Buff过期逻辑
 * 4. 触发单位所有被动/主动技能
 * 
 * 属性说明：
 * - id: 单位的唯一标识符
 * - type: 单位类型（士兵类型枚举）
 * - hp/maxHp: 当前生命值/最大生命值
 * - baseAtk/baseDef: 基础攻防值（不含Buff修饰）
 * - atk/def: 已弃用字段，使用getAtk()/getDef()方法代替
 * - buffs: 当前作用在单位上的增益/减益效果列表
 * - skills: 单位拥有的技能列表（英雄单位可能包含多个技能）
 * 
 * 工作流程：
 * 1. 每回合调用tickBuffs()更新Buff状态
 * 2. 通过getAtk()/getDef()获取实时攻防值（已应用Buff修饰）
 * 3. 通过canAct()检查单位是否被控制
 * 4. 通过triggerSkills()触发符合条件的技能
 * 
 * @author Winter Server author
 */
@Getter
@Setter
public class BattleUnit {
    private int id;
    private SoldiersType type;
    private int hp;
    private int maxHp;
    private int baseAtk; // 基础攻击 (不含Buff)
    private int baseDef; // 基础防御

    // 动态状态
    private List<Buff> buffs = new ArrayList<>();

    // 英雄技能 (如果是英雄单位)
    private List<GeneralSkill> skills = new ArrayList<>();

    public boolean isDead() {
        return hp <= 0;
    }

    // 扣血逻辑
    public void takeDamage(int dmg) {
        int remaining = Math.max(0, dmg);

        // 护盾优先抵挡伤害：SHIELD.value 视为“可吸收的伤害值”
        if (remaining > 0 && !buffs.isEmpty()) {
            Iterator<Buff> it = buffs.iterator();
            while (it.hasNext() && remaining > 0) {
                Buff b = it.next();
                // 只有 SHIELD 类型的 Buff 能抵挡伤害
                if (!b.isShield()) {
                    continue;
                }

                // SHIELD 的 value 表示剩余的可吸收伤害值
                int shield = Math.max(0, b.getValue());
                if (shield <= 0) {
                    it.remove();
                    continue;
                }

                int absorbed = Math.min(shield, remaining);
                remaining -= absorbed;

                // 更新 SHIELD 的剩余吸收值
                shield -= absorbed;
                b.setValue(shield);

                if (shield <= 0) {
                    it.remove();
                }
            }
        }
        this.hp -= remaining;
        if (this.hp < 0)
            this.hp = 0;
    }

    /**
     * 回合开始结算：处理所有 tickTiming=ROUND_START 的 DOT/HOT
     */
    public void applyRoundStartBuffEffects() {
        applyTimingBuffEffects("ROUND_START");
    }

    /**
     * 回合结束结算：处理所有 tickTiming=ROUND_END 的 DOT/HOT
     */
    public void applyRoundEndBuffEffects() {
        applyTimingBuffEffects("ROUND_END");
    }

    /**
     * 根据结算时机统一处理 DOT（持续伤害）和 HOT（持续恢复）
     */
    private void applyTimingBuffEffects(String timing) {
        int dotDamage = 0;
        int hotHeal = 0;
        for (Buff b : this.buffs) {
            BuffConfig cfg = b.getConfig();
            if (cfg == null || !cfg.matchTiming(timing)) continue;

            if (cfg.isDot()) {
                dotDamage += Math.max(0, b.getValue());
            } else if (cfg.getBuffCategory() == com.winter.modules.battle.config.BuffCategory.HOT) {
                hotHeal += Math.max(0, b.getValue());
            }
        }
        if (dotDamage > 0) {
            takeDamage(dotDamage);
        }
        if (hotHeal > 0) {
            this.hp = Math.min(this.maxHp, this.hp + hotHeal);
        }
    }

    // --- 核心：添加 Buff ---
    public void addBuff(Buff buff) {
        if (buff == null) {
            return;
        }

        BuffConfig cfg = buff.getConfig();

        // 先检查是否已有同 buffId 的 Buff
        for (Buff existing : this.buffs) {
            if (existing.getBuffId() == buff.getBuffId()) {
                if (cfg != null && cfg.isStackable()) {
                    // 可叠加：数值累加，回合取较大值
                    existing.setValue(existing.getValue() + buff.getValue());
                    existing.setRounds(Math.max(existing.getRounds(), buff.getRounds()));
                } else {
                    // 不可叠加：刷新持续时间和数值
                    existing.setValue(buff.getValue());
                    existing.setRounds(buff.getRounds());
                }
                existing.setSourceId(buff.getSourceId());
                return;
            }
        }

        // 没有同 buffId，直接添加
        this.buffs.add(buff);
    }

    // --- 核心：每回合更新 Buff ---
    public void tickBuffs() {
        /**
         * 创建一个用于遍历buffs集合的迭代器。
         * 通过Iterator迭代器可以安全地遍历和移除集合中的Buff元素。
         */
        Iterator<Buff> it = buffs.iterator();
        while (it.hasNext()) {
            Buff b = it.next();
            b.tick(); // 回合数 -1
            if (b.isExpired()) {
                it.remove(); // 移除过期 Buff
            }
        }
    }

    // --- 核心：动态获取攻击力 ---
    public int getAtk() {
        double modifier = getAttrModifier("ATK");
        return (int) (this.baseAtk * Math.max(0.1, modifier));
    }

    // --- 核心：动态获取防御力 ---
    public int getDef() {
        double modifier = getAttrModifier("DEF");
        return (int) (this.baseDef * Math.max(0.1, modifier));
    }

    /**
     * 通用属性倍率计算：遍历所有 ATTR_MOD 类 Buff，
     * 按 attrType 匹配，positive=true 加，false 减
     */
    private double getAttrModifier(String attrType) {
        double modifier = 1.0;
        for (Buff b : buffs) {
            if (!b.isAttrMod()) continue;
            BuffConfig cfg = b.getConfig();
            if (cfg == null || !attrType.equalsIgnoreCase(cfg.getAttrType())) continue;

            if (cfg.isPositive()) {
                modifier += (b.getValue() / 100.0);
            } else {
                modifier -= (b.getValue() / 100.0);
            }
        }
        return modifier;
    }

    // 判断是否被控制
    public boolean canAct() {
        for (Buff b : buffs) {
            if (b.isControl()) return false;
        }
        return true;
    }

    public void addSkill(GeneralSkill skill) {
        this.skills.add(skill);
    }

    // 技能冷却时间减1
    public void tickSkillCooldowns() {
        for (GeneralSkill skill : skills) {
            skill.tickCooldown();
        }
    }

    // 主动减少技能冷却时间（例如某些技能可以缩短其他技能的CD）
    public void reduceSkillCooldowns(int rounds) {
        for (GeneralSkill skill : skills) {
            skill.reduceCooldown(rounds);
        }
    }

    /**
     * 核心：触发所有技能
     *
     * @return 本次触发到的第一个技能ID；未触发返回0
     */
    public int triggerSkills(SkillTrigger trigger, BattleContext ctx) {
        int firstTriggeredSkillId = 0;
        for (GeneralSkill skill : skills) {
            if (skill.canTrigger(trigger, ctx)) {
                skill.execute(ctx);
                if (firstTriggeredSkillId == 0) {
                    firstTriggeredSkillId = skill.getSkillId();
                }
            }
        }
        return firstTriggeredSkillId;
    }
}