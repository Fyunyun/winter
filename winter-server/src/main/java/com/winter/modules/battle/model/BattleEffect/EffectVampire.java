package com.winter.modules.battle.model.BattleEffect;

import com.winter.modules.battle.core.BattleContext;

public class EffectVampire implements BattleEffect {
    @Override
    public void apply(BattleContext ctx, int param1, int param2) {
        if (ctx.source == null || ctx.source.isDead()) {
            return;
        }

        int damage = Math.max(0, ctx.finalDamage);
        int heal = (int) Math.round(damage * Math.max(0, param1) / 100.0);
        if (heal <= 0) {
            return;
        }

        int nextHp = Math.min(ctx.source.getMaxHp(), ctx.source.getHp() + heal);
        ctx.source.setHp(nextHp);
    }
}
