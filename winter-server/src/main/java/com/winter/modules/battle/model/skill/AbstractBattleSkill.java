package com.winter.modules.battle.model.skill;

import com.winter.modules.battle.core.BattleContext;

public abstract class AbstractBattleSkill implements BattleSkill {
    protected int skillId;
    protected SkillTrigger myTriggerPhase; // 这个技能配置的触发时机
    protected double probability; // 触发概率 (0.0 - 1.0)
    protected int cooldown; // 冷却回合
    protected int currentCd = 0; // 当前冷却

    // 构造函数从配置表读取这些值
    public AbstractBattleSkill(int skillId, SkillTrigger trigger, double prob, int cd) {
        this.skillId = skillId;
        this.myTriggerPhase = trigger;
        this.probability = prob;
        this.cooldown = cd;
    }

    @Override
    public int getSkillId() {
        return skillId;
    }

    @Override
    public boolean canTrigger(SkillTrigger trigger, BattleContext ctx) {
        // --- 第一层过滤：时机是否匹配 ---
        // 如果现在是“回合开始”，而我是“死亡时”触发的技能，直接滚粗
        if (trigger != this.myTriggerPhase) {
            return false;
        }

        // --- 第二层过滤：硬控状态检查 ---
        // 如果技能所有者被“眩晕(Stun)”或“沉默(Silence)”，且这不是一个解控技能
        if (!ctx.source.canAct()) {
            return false;
        }

        // --- 第三层过滤：冷却时间 (CD) ---
        if (currentCd > 0) {
            return false;
        }

        // --- 第四层过滤：概率 (Random) ---
        // 必须使用 ctx 里的 random，保证回放一致性
        if (probability < 1.0 && ctx.random.nextDouble() > probability) {
            return false; // 脸黑，没触发
        }

        // --- 第五层过滤：特殊业务逻辑 (留给子类实现) ---
        // 比如：只有血量低于 30% 才能触发
        return checkSpecificConditions(ctx);
    }

    /**
     * 钩子方法：子类去实现具体的特殊判断
     */
    protected abstract boolean checkSpecificConditions(BattleContext ctx);

    @Override
    public void execute(BattleContext ctx) {
        // 执行前重置 CD
        this.currentCd = this.cooldown;
        doExecute(ctx);
    }

    public void tickCooldown() {
        if (this.currentCd > 0) {
            this.currentCd--;
        }
    }

    public void reduceCooldown(int rounds) {
        if (rounds <= 0) {
            return;
        }
        this.currentCd = Math.max(0, this.currentCd - rounds);
    }

    protected abstract void doExecute(BattleContext ctx);
}
