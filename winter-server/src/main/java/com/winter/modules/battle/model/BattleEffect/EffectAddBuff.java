package com.winter.modules.battle.model.BattleEffect;

import com.winter.modules.battle.core.BattleContext;
import com.winter.modules.battle.config.BuffConfig;
import com.winter.modules.battle.config.BuffConfigTable;
import com.winter.modules.battle.model.BattleUnit;
import com.winter.modules.battle.model.skill.Buff;

/**
 * 效果：添加 Buff
 *
 * param1 = buffId（对应 buff_config.json 中的 buffId）
 * param2 = 持续回合数
 */
public class EffectAddBuff implements BattleEffect {
    @Override
    public void apply(BattleContext ctx, int param1, int param2) {
        int buffId = param1;
        BuffConfig config = BuffConfigTable.getInstance().get(buffId);
        if (config == null) {
            return;
        }

        // 增益 Buff 加给自己，减益 Buff 加给目标
        BattleUnit receiver;
        if (config.isPositive()) {
            receiver = ctx.source;
        } else {
            receiver = ctx.target;
        }

        if (receiver == null || receiver.isDead()) {
            return;
        }

        int rounds = param2 > 0 ? param2 : 2;
        int value = config.getDefaultValue();

        Buff buff = new Buff(buffId, value, rounds, ctx.source != null ? ctx.source.getId() : 0);
        receiver.addBuff(buff);
    }
}
