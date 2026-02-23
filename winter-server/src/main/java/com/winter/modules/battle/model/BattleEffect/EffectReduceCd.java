package com.winter.modules.battle.model.BattleEffect;

import com.winter.modules.battle.core.BattleContext;

public class EffectReduceCd implements BattleEffect {
    @Override
    public void apply(BattleContext ctx, int param1, int param2) {
        if (ctx.source == null || ctx.source.isDead()) {
            return;
        }
        int rounds = Math.max(1, param1);
        ctx.source.reduceSkillCooldowns(rounds);
    }
}
