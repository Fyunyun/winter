package com.winter.modules.battle.config;

import com.winter.modules.battle.core.BattleContext;
import com.winter.modules.battle.core.ConditionChecker;
import com.winter.modules.battle.model.BattleUnit;
import com.winter.modules.battle.model.skill.Buff;

/**
 * 通用条件检查器 —— 根据 ConditionType 和参数实现各种条件判断
 * 
 * 由配置表驱动，运行时通过 {@link SkillFactory} 实例化。
 */
public class GeneralConditionChecker implements ConditionChecker {

    private final ConditionType conditionType;
    private final int param;

    public GeneralConditionChecker(ConditionType conditionType, int param) {
        this.conditionType = conditionType;
        this.param = param;
    }

    @Override
    public boolean check(BattleContext ctx, int[] params) {
        // 使用自身字段 conditionType 和 param 进行判断
        if (conditionType == null) {
            return true;
        }

        switch (conditionType) {
            case NONE:
                return true;

            case SELF_HP_BELOW:
                // param = 百分比，如 30 代表 30%
                return hpPercent(ctx.getSource()) < param;

            case SELF_HP_ABOVE:
                return hpPercent(ctx.getSource()) > param;

            case TARGET_HP_BELOW:
                return ctx.getTarget() != null && hpPercent(ctx.getTarget()) < param;

            case TARGET_HP_ABOVE:
                return ctx.getTarget() != null && hpPercent(ctx.getTarget()) > param;

            case ROUND_GE:
                return ctx.getRound() >= param;

            case ROUND_LE:
                return ctx.getRound() <= param;

            case TARGET_HAS_BUFF:
                return ctx.getTarget() != null && hasBuff(ctx.getTarget(), param);

            case SELF_HAS_BUFF:
                return hasBuff(ctx.getSource(), param);

            case TARGET_TYPE_IS:
                return ctx.getTarget() != null
                        && ctx.getTarget().getType() != null
                        && ctx.getTarget().getType().getValue() == param;

            case SELF_BUFF_COUNT_GE:
                return ctx.getSource().getBuffs() != null
                        && ctx.getSource().getBuffs().size() >= param;

            default:
                return true;
        }
    }

    // ======================== 工具方法 ========================

    private double hpPercent(BattleUnit unit) {
        if (unit.getMaxHp() <= 0) return 0;
        return (double) unit.getHp() / unit.getMaxHp() * 100.0;
    }

    private boolean hasBuff(BattleUnit unit, int buffId) {
        if (unit.getBuffs() == null) return false;
        for (Buff b : unit.getBuffs()) {
            if (b.getBuffId() == buffId && !b.isExpired()) {
                return true;
            }
        }
        return false;
    }
}
