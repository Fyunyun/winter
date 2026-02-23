package com.winter.modules.battle.model.skill;

import com.winter.modules.battle.core.BattleContext;
import java.util.List;
import com.winter.modules.battle.core.ConditionChecker;
import com.winter.modules.battle.core.EffectFactory;
import lombok.Getter;
import lombok.Setter;
import com.winter.modules.battle.model.BattleEffectConfig;
import com.winter.modules.battle.model.BattleEffect.BattleEffect;

import java.util.Collections;

@Getter
@Setter
// 这种设计下，你只需要一个类就能支持几百个技能
public class GeneralSkill extends AbstractBattleSkill {
    // 这是一个条件列表，从 JSON/XML 配置里加载
    // 比如：ConditionType.TARGET_HP_BELOW, params: [20]
    private List<ConditionChecker> conditions;

    private List<BattleEffectConfig> effectConfigs;

    public GeneralSkill() {
        super(0, SkillTrigger.ROUND_START, 1.0, 0); // 这些值在实际使用中不重要，因为 canTrigger 和 doExecute 都被重写了
    }

    public GeneralSkill(int skillId, SkillTrigger trigger, double probability, int cooldown,
            List<ConditionChecker> conditions, List<BattleEffectConfig> effectConfigs) {
        super(skillId, trigger, probability, cooldown);
        this.conditions = (conditions == null) ? Collections.emptyList() : conditions;
        this.effectConfigs = (effectConfigs == null) ? Collections.emptyList() : effectConfigs;
    }


    // 特殊条件检查：除了基础的时机、概率、冷却等，还要检查配置的条件
    @Override
    protected boolean checkSpecificConditions(BattleContext ctx) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }
        for (ConditionChecker checker : conditions) {
            if (!checker.check(ctx, null)) {
                return false; // 只要有一个条件不满足，就不触发
            }
        }
        return true;
    }

    @Override
    public void doExecute(BattleContext ctx) {
        if (effectConfigs == null || effectConfigs.isEmpty()) {
            return;
        }
        // 遍历所有配置好的效果
        for (BattleEffectConfig config : effectConfigs) {

            // 1. 找到对应的效果处理器 (可以使用工厂模式或 Map 缓存)
            BattleEffect effect = EffectFactory.get(config.getType());

            // 2. 执行效果
            if (effect != null) {
                effect.apply(ctx, config.getParam1(), config.getParam2());
            }
        }

        // 3. 记录技能释放 Action (为了回放)
        // 告诉 Context：“我释放了技能 ID 1001”
        ctx.recordSkillCast(this.skillId);
    }
}