package com.winter.modules.battle.model.BattleEffect;

import com.winter.modules.battle.core.BattleContext;
import com.winter.modules.battle.model.BattleUnit;

public class EffectHeal implements BattleEffect {
    @Override
    public void apply(BattleContext ctx, int param1, int param2) {
        BattleUnit unit = ctx.source;
        if (param2 == 1 && ctx.target != null) {
            unit = ctx.target;
        }
        if (unit == null || unit.isDead()) {
            return;
        }

        int heal = Math.max(0, param1);
        int nextHp = Math.min(unit.getMaxHp(), unit.getHp() + heal);
        unit.setHp(nextHp);
    }
}
