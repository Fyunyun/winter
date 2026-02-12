package com.winter.modules.battle.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import java.util.Iterator;
import com.winter.modules.battle.core.BattleContext;

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
    private List<BattleSkill> skills = new ArrayList<>();

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
                // 只有 SHIELD 类型的 Buff 能抵挡伤害 不是 SHIELD 的 Buff 不处理
                if (b.getType() != BuffType.SHIELD) {
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
     * 回合开始结算：例如中毒（POISON）
     * 注意：这里默认持续伤害会被护盾吸收（因为复用 takeDamage）。
     */
    public void applyRoundStartBuffEffects() {
        int dotDamage = 0;
        for (Buff b : this.buffs) {
            if (b.getType() == BuffType.POISON) {
                dotDamage += Math.max(0, b.getValue());
            }
        }
        if (dotDamage > 0) {
            takeDamage(dotDamage);
        }
    }

    /**
     * 回合结束结算：例如燃烧（BURN）
     */
    public void applyRoundEndBuffEffects() {
        int dotDamage = 0;
        for (Buff b : this.buffs) {
            if (b.getType() == BuffType.BURN) {
                dotDamage += Math.max(0, b.getValue());
            }
        }
        if (dotDamage > 0) {
            takeDamage(dotDamage);
        }
    }

    // --- 核心：添加 Buff ---
    public void addBuff(Buff buff) {
        // 同类型合并（叠层）
        if (buff == null) {
            return;
        }

        // 先检查是否已有同类型 Buff 有的话直接修改然后返回
        for (Buff existing : this.buffs) {
            if (existing.getType() == buff.getType()) {
                existing.setValue(existing.getValue() + buff.getValue());
                existing.setRounds(existing.getRounds() + buff.getRounds());
                // 使用最新一次施加的配置/来源信息
                existing.setConfigId(buff.getConfigId());
                existing.setSourceId(buff.getSourceId());
                return;
            }
        }

        // 没有同类型 Buff，直接添加
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
        double modifier = 1.0;

        for (Buff b : buffs) {
            switch (b.getType()) {
                case ATK_UP:
                    modifier += (b.getValue() / 100.0);
                    break;
                case ATK_DOWN:
                    modifier -= (b.getValue() / 100.0);
                    break;
                default:
                    break; // 其他 Buff 不影响攻击力
            }
        }

        // 简单公式：基础 * 倍率
        return (int) (this.baseAtk * Math.max(0.1, modifier)); // 至少保留10%攻击力
    }

    // 同样的方法写 getDef()...
    public int getDef() {
        double modifier = 1.0;

        for (Buff b : buffs) {
            switch (b.getType()) {
                case DEF_UP:
                    modifier += (b.getValue() / 100.0);
                    break;
                case DEF_DOWN:
                    modifier -= (b.getValue() / 100.0);
                    break;
                default:
                    break; // 其他 Buff 不影响防御力
            }
        }

        return (int) (this.baseDef * Math.max(0.1, modifier));
    }

    // 判断是否被控制
    public boolean canAct() {
        for (Buff b : buffs) {
            if (b.getType() == BuffType.STUN)
                return false;
        }
        return true;
    }

    public void addSkill(BattleSkill skill) {
        this.skills.add(skill);
    }

    /**
     * 核心：触发所有技能
     * 
     * @return 如果有技能修改了伤害，返回修改后的值；否则返回原值
     */
    public void triggerSkills(SkillTrigger trigger, BattleContext ctx) {
        for (BattleSkill skill : skills) {
            if (skill.canTrigger(trigger, ctx)) {
                // 执行技能
                skill.execute(ctx);

                // 记录日志 (可选，用于回放)
                // System.out.println("Unit " + id + " triggered skill " + skill.getSkillId());
            }
        }
    }
}