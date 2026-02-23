package com.winter.modules.battle.model.BattleEffect;

import com.winter.modules.battle.core.BattleContext;

/**
 * 战斗效果原子执行器接口
 */
public interface BattleEffect {
    /**
     * 执行具体的技能效果
     * @param ctx 战斗上下文 (用于修改伤害、获取随机数等)
     * @param param1 配置表参数1
     * @param param2 配置表参数2
     */
    void apply(BattleContext ctx, int param1, int param2);
}