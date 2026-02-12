package com.winter.modules.battle.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 战斗行动信息类
 * 
 * 用于表示战斗中的单次行动，记录行动者、目标、伤害等信息。
 */
@AllArgsConstructor
@Getter
@Setter
public class BattleAction {
    /**
     * 行动所在的战斗轮次
     * 用于标识这次行动发生在第几轮战斗中
     */
    private int round;
    
    /**
     * 行动者的唯一标识符
     * 表示执行此行动的角色或单位的ID
     */
    private long actorId;
    
    /**
     * 目标的唯一标识符
     * 表示受此行动影响的角色或单位的ID
     */
    private long targetId;
    
    /**
     * 本次行动造成的伤害值
     * 数值为非负数，表示对目标造成的伤害程度
     */
    private int damage;
    
    /**
     * 使用的技能ID
     * 0 表示普通攻击，大于0表示使用具体的技能ID
     */
    private int skillId;
}
