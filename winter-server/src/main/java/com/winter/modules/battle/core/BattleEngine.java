package com.winter.modules.battle.core;

import com.winter.modules.battle.model.BattleAction;
import com.winter.modules.battle.model.BattleGroup;
import com.winter.modules.battle.model.SkillTrigger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import com.winter.modules.battle.model.BattleUnit;
import com.winter.modules.battle.model.BattleResult;
import java.util.stream.Collectors;

public class BattleEngine {
    private Random random;
    private final long seed;
    private BattleGroup attacker;
    private BattleGroup defender;
    private final List<String> battleLogs = new ArrayList<>(); // 简易战报
    private final List<BattleAction> actions = new ArrayList<>(); // 动作序列（用于回放/结构化战报）
    private int round = 0;

    public BattleEngine(BattleGroup atk, BattleGroup def, long seed) {
        this.attacker = atk;
        this.defender = def;
        this.seed = seed;
        this.random = new Random(seed); // 【关键】使用固定种子
    }

    public BattleResult simulate() {
        // 战斗主循环
        while (!isOver() && round < 100) { // 防止死循环
            round++;
            processRound();
        }
        return new BattleResult(attacker.isAlive(), seed, battleLogs, actions);
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
    }

    private void executeAttack(BattleGroup source, BattleGroup target) {
        // 寻找攻击者：必须存活且可行动（如未被 STUN）
        List<BattleUnit> canActUnits = source.getUnits().stream()
            .filter(u -> !u.isDead() && u.canAct())
            .collect(Collectors.toList());
        BattleUnit atkUnit = canActUnits.isEmpty()
            ? null
            : canActUnits.get(this.random.nextInt(canActUnits.size()));

        // 寻找目标 (可以是随机，必须用 this.random)
        BattleUnit defUnit = target.getRandomAliveUnit(this.random);

        if (atkUnit == null || defUnit == null)
            return;

        // 计算伤害
        int damage = DamageCalculator.calc(atkUnit, defUnit, this.random);

        // 应用伤害
        defUnit.takeDamage(damage);

        // 记录战报 (Proto格式)
        recordAction(atkUnit.getId(), defUnit.getId(), damage);
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
            double critMod = isCrit ? 1.5 : 1.0;

            // 4. 最终伤害
            return (int) (rawDmg * counterMod * critMod);
        }
    }

    /**
     * 记录每一个战斗动作
     */
    private void recordAction(long actorId, long targetId, int damage) {
        recordAction(actorId, targetId, damage, 0);
    }

    private void recordAction(long actorId, long targetId, int damage, int skillId) {
        // 1. 打印到控制台日志（方便开发调试）
        String log = String.format("第%d回合: [%d] 攻击了 [%d], 造成伤害: %d (技能:%d)",
                this.round, actorId, targetId, damage, skillId);
        this.battleLogs.add(log);

        this.actions.add(new BattleAction(this.round, actorId, targetId, damage, skillId));
    }
    
}