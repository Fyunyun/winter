package com.winter.modules.battle.model.skill;

import com.winter.modules.battle.core.BattleContext;

public interface BattleSkill {
    
    /**
     * 获取技能ID (对应配置表)
     */
    int getSkillId();

    /**
     * 判断技能是否能触发
     * @param trigger 当前的触发时机
     * @param ctx 战斗上下文
     */
    boolean canTrigger(SkillTrigger trigger, BattleContext ctx);

    /**
     * 执行技能逻辑
     * @param ctx 战斗上下文 (可以在这里修改 damage，或者给 target 加 Buff)
     */
    void execute(BattleContext ctx);
}