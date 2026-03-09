package com.winter.modules.battle.core;

import com.winter.modules.battle.model.BattleGroup;
import com.winter.msg.BattleMsg;
import com.winter.msg.BattleMsg.BattleGroupInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import com.winter.modules.battle.model.BattleUnit;
import com.winter.modules.battle.model.skill.SkillTrigger;
import com.winter.modules.battle.model.BattleResult;
import java.util.stream.Collectors;

public class BattleEngine {
    private Random random; // 战斗随机源（必须固定种子以保证可回放）
    private final long seed; // 本场战斗的随机种子
    private BattleGroup attacker;
    private BattleGroup defender;
    private final int maxRounds;
    private final List<String> battleLogs = new ArrayList<>(); // 简易战报
    private final List<BattleMsg.BattleAction> actions = new ArrayList<>(); // Protobuf 动作序列（用于回放/结构化战报）
    private int round = 0;

    // 所以到时候只需要创建 BattleEngine 实例，传入双方阵营和随机种子，然后调用 simulate() 方法即可

    public BattleEngine(BattleGroup atk, BattleGroup def, long seed) {
        this(atk, def, seed, 100);
    }

    public BattleEngine(BattleGroup atk, BattleGroup def, long seed, int maxRounds) {
        this.attacker = atk;
        this.defender = def;
        this.seed = seed;
        this.maxRounds = Math.max(1, maxRounds);
        this.random = new Random(seed); // 【关键】使用固定种子
    }

    public BattleResult simulate() {

        BattleMsg.BattleGroupInfo atk = buildGroupInfo(this.attacker);
        BattleMsg.BattleGroupInfo def = buildGroupInfo(this.defender);

        // 战斗开始：触发 BATTLE_START 技能
        for (BattleUnit unit : attacker.getUnits()) {
            if (!unit.isDead()) {
                unit.triggerSkills(SkillTrigger.BATTLE_START, new BattleContext(unit, null, this.random, 0));
            }
        }
        for (BattleUnit unit : defender.getUnits()) {
            if (!unit.isDead()) {
                unit.triggerSkills(SkillTrigger.BATTLE_START, new BattleContext(unit, null, this.random, 0));
            }
        }

        // 战斗主循环
        while (!isOver() && round < maxRounds) { // 防止死循环
            round++;
            processRound();
        }
        // 构建 Protobuf BattleRecord
        BattleMsg.BattleRecord record = BattleMsg.BattleRecord.newBuilder()
                .setSeed(seed)
                .setAttacker(atk)
                .setDefender(def)
                .addAllActions(actions)
                .build();
        return new BattleResult(attacker.isAlive(), seed, battleLogs, record);
    }

    private void processRound() {
        // 1. 回合开始：结算持续效果 + 触发回合开始技能
        for (BattleUnit unit : attacker.getUnits()) {
            if (unit.isDead()) {
                continue;
            }
            unit.applyRoundStartBuffEffects();
            unit.triggerSkills(SkillTrigger.ROUND_START, new BattleContext(unit, null, this.random, this.round));
        }
        for (BattleUnit unit : defender.getUnits()) {
            if (unit.isDead()) {
                continue;
            }
            unit.applyRoundStartBuffEffects();
            unit.triggerSkills(SkillTrigger.ROUND_START, new BattleContext(unit, null, this.random, this.round));
        }

        // 2. 双方轮流攻击
        // 简单模型：攻击方先手
        executeAttack(attacker, defender);
        if (defender.isAlive()) {
            executeAttack(defender, attacker);
        }

        // 3. 回合结束：结算持续效果 + 触发回合结束技能
        for (BattleUnit unit : attacker.getUnits()) {
            if (unit.isDead()) {
                continue;
            }
            unit.applyRoundEndBuffEffects();
            unit.triggerSkills(SkillTrigger.ROUND_END, new BattleContext(unit, null, this.random, this.round));
        }
        for (BattleUnit unit : defender.getUnits()) {
            if (unit.isDead()) {
                continue;
            }
            unit.applyRoundEndBuffEffects();
            unit.triggerSkills(SkillTrigger.ROUND_END, new BattleContext(unit, null, this.random, this.round));
        }

        // 4. 回合末统一扣除持续回合并清理过期 Buff
        attacker.tickBuffs();
        defender.tickBuffs();
        attacker.tickSkillCooldowns();
        defender.tickSkillCooldowns();
    }

    private void executeAttack(BattleGroup source, BattleGroup target) {
        // 寻找攻击者：必须存活且可行动（如未被 STUN）
        List<BattleUnit> canActUnits = source.getUnits().stream()
            .filter(u -> !u.isDead() && u.canAct())
            .collect(Collectors.toList());

        // 随机选一个能行动的单位攻击
        BattleUnit atkUnit = canActUnits.isEmpty()
            ? null
            : canActUnits.get(this.random.nextInt(canActUnits.size()));

        // 寻找目标 (可以是随机，必须用 this.random)
        BattleUnit defUnit = target.getRandomAliveUnit(this.random);

        if (atkUnit == null || defUnit == null)
            return;

        BattleContext attackCtx = new BattleContext(atkUnit, defUnit, this.random, this.round);

        // 普通攻击伤害计算
        int damage = DamageCalculator.calc(atkUnit, defUnit, this.random);
        attackCtx.rawDamage = damage;
        attackCtx.finalDamage = damage;

        // 攻击前技能：可修改伤害/附加效果
        int usedSkillId = atkUnit.triggerSkills(SkillTrigger.BEFORE_ATTACK, attackCtx);
        damage = Math.max(0, attackCtx.finalDamage);

        // 应用伤害
        defUnit.takeDamage(damage);

        // 被击中与攻击后技能
        BattleContext onDamagedCtx = new BattleContext(defUnit, atkUnit, this.random, this.round);
        onDamagedCtx.rawDamage = damage;
        onDamagedCtx.finalDamage = damage;
        defUnit.triggerSkills(SkillTrigger.ON_DAMAGED, onDamagedCtx);

        BattleContext afterAttackCtx = new BattleContext(atkUnit, defUnit, this.random, this.round);
        afterAttackCtx.rawDamage = damage;
        afterAttackCtx.finalDamage = damage;
        atkUnit.triggerSkills(SkillTrigger.AFTER_ATTACK, afterAttackCtx);

        // 记录战报 (Proto格式)
        recordAction(atkUnit.getId(), defUnit.getId(), damage, usedSkillId);
    }

    // 新增：战斗是否结束
    private boolean isOver() {
        // 任意一方不存活则结束
        return attacker == null || defender == null || !attacker.isAlive() || !defender.isAlive();
    }

    public class DamageCalculator {
        public static int calc(BattleUnit atk, BattleUnit def, Random rnd) {
            // 1. 基础攻防公式
            double rawDmg = Math.max(1, atk.getAtk() - def.getDef());

            // 2. 兵种克制
            double counterMod = atk.getType().getCounterMod(def.getType());

            // 3. 暴击 (随机数必须由 Engine 传入)
            boolean isCrit = rnd.nextDouble() < 0.2; // 假设20%暴击率
            double critMod = isCrit ? 1.2 : 1.0;

            // 4. 最终伤害
            return (int) (rawDmg * counterMod * critMod);
        }
    }

    /**
     * 记录每一个战斗动作
     */
    // private void recordAction(long actorId, long targetId, int damage) {
    //     recordAction(actorId, targetId, damage, 0);
    // }

    private void recordAction(long actorId, long targetId, int damage, int skillId) {
        recordAction(actorId, targetId, damage, false, skillId);
    }

    private void recordAction(long actorId, long targetId, int damage, boolean isCrit, int skillId) {
        // 1. 打印到控制台日志（方便开发调试）
        String log = String.format("第%d回合: [%d] 攻击了 [%d], 造成伤害: %d%s (技能:%d)",
                this.round, actorId, targetId, damage, isCrit ? " [暴击]" : "", skillId);
        this.battleLogs.add(log);

        // 2. 构建 Protobuf Action
        BattleMsg.BattleAction action = BattleMsg.BattleAction.newBuilder()
                .setRound(this.round)
                .setActorId((int) actorId)
                .setTargetId((int) targetId)
                .setDamage(damage)
                .setIsCrit(isCrit)
                .setSkillId(skillId)
                .build();
        this.actions.add(action);
    }

    /**
     * 将 BattleGroup 转换为 Protobuf BattleGroupInfo（含初始快照）
     */
    private static BattleMsg.BattleGroupInfo buildGroupInfo(BattleGroup group) {
        BattleMsg.BattleGroupInfo.Builder builder = BattleMsg.BattleGroupInfo.newBuilder()
                .setGroupName(group.getGroupName());
        for (BattleUnit unit : group.getUnits()) {
            builder.addUnits(BattleMsg.UnitSnapshot.newBuilder()
                    .setUnitId(unit.getId())
                    .setConfigId(unit.getType() != null ? unit.getType().getValue() : 0)
                    .setHp(unit.getHp())
                    .setMaxHp(unit.getMaxHp())
                    .setLevel(1) // 可扩展
                    .build());
        }
        return builder.build();
    }
}