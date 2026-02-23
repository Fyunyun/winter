package com.winter.modules.battle.core;

public interface ConditionChecker {
    /**
     * @param ctx    战斗上下文
     * @param params 配置表传过来的参数 (比如 [20, 0])
     * @return 是否满足条件
     */
    boolean check(BattleContext ctx, int[] params);
}