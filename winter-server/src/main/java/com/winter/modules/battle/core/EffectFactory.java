package com.winter.modules.battle.core;

import com.winter.modules.battle.model.EffectType;
import com.winter.modules.battle.model.BattleEffect.BattleEffect;
import com.winter.modules.battle.model.BattleEffect.EffectAddBuff;
import com.winter.modules.battle.model.BattleEffect.EffectHeal;
import com.winter.modules.battle.model.BattleEffect.EffectModDamage;
import com.winter.modules.battle.model.BattleEffect.EffectReduceCd;
import com.winter.modules.battle.model.BattleEffect.EffectVampire;

import java.util.HashMap;
import java.util.Map;

public class EffectFactory {
    // 缓存单例效果对象
    private static final Map<EffectType, BattleEffect> effects = new HashMap<>();

    static {
        // 在这里注册所有的效果实现类
        effects.put(EffectType.MOD_DAMAGE, new EffectModDamage());
        effects.put(EffectType.HEAL, new EffectHeal());
        effects.put(EffectType.VAMPIRE, new EffectVampire());
        effects.put(EffectType.ADD_BUFF, new EffectAddBuff());
        effects.put(EffectType.REDUCE_CD, new EffectReduceCd());
        // ... 注册其他效果
    }

    public static BattleEffect get(EffectType type) {
        return effects.get(type);
    }
}