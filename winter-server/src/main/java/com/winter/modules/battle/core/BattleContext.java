package com.winter.modules.battle.core; // 注意包名，建议放在 core

import com.winter.modules.battle.model.BattleUnit;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Random;

@Getter
@Setter
@AllArgsConstructor
public class BattleContext {
    public final BattleUnit source;   // 技能释放者
    public final BattleUnit target;   // 技能目标 (如果有)
    public final Random random;       // 随机数生成器 (必须传进来，保证回放一致)
    public int round;           // 当前回合数

    // 动态信息 (可读写 - 随管道流动而变化)
    public int rawDamage;       // 原始计算伤害
    public int finalDamage;     // 最终应扣伤害
    public boolean isCrit;      // 是否暴击
    public boolean isHit;       // 是否命中
    public boolean isDodged;    // 是否闪避

    // 构造函数
    public BattleContext(BattleUnit source, BattleUnit target, Random random, int round) {
        this.source = source;
        this.target = target;
        this.random = random;
        this.round = round;
    }

    /**
     * 记录技能释放（用于回放）
     */
    public void recordSkillCast(int skillId) {
        // 这里可以扩展成记录更多信息，比如目标、伤害等
        // 但目前先简单记录技能 ID 就行了
        // 这个方法会被 GeneralSkill 调用，告诉 Context “我释放了技能 ID xxx”
        System.out.println("Skill cast: " + skillId);
    }
}