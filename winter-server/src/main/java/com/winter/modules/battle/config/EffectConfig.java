package com.winter.modules.battle.config;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 单条技能效果配置
 * 
 * 示例：
 *   { "type": 1, "param1": 50, "param2": 0 }
 *   表示 MOD_DAMAGE → 增加 50% 伤害
 * 
 *   { "type": 3, "param1": 14, "param2": 2 }
 *   表示 ADD_BUFF → 施加 STUN(14) 持续 2 回合
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EffectConfig {
    /** 效果类型 ID，对应 {@link com.winter.modules.battle.model.EffectType} */
    private int type;
    /** 参数1（数值/百分比/buffId 等） */
    private int param1;
    /** 参数2（持续回合/子参数等） */
    private int param2;
}
