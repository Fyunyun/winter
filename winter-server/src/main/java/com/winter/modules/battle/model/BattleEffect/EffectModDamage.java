package com.winter.modules.battle.model.BattleEffect;

import com.winter.modules.battle.core.BattleContext;

public class EffectModDamage implements BattleEffect {
    @Override
    public void apply(BattleContext ctx, int param1, int param2) {
        int base = Math.max(0, ctx.finalDamage);
        int scaled = (int) Math.round(base * (100 + param1) / 100.0);
        ctx.finalDamage = Math.max(0, scaled);
    }
}
