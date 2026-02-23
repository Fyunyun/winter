package com.winter.modules.battle.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class BattleGroup {
    // 阵营名：如“攻击方”、“防守方”
    private String groupName;
    // 阵营内所有战斗单位
    private List<BattleUnit> units = new ArrayList<>();

    public BattleGroup(String name) {
        this.groupName = name;
    }

    public void addUnit(BattleUnit unit) {
        this.units.add(unit);
    }

    /**
     * 判断整支部队是否还活着（只要有一个单位活着就没灭绝）
     */
    public boolean isAlive() {
        return units.stream().anyMatch(unit -> !unit.isDead());
    }

    /**
     * 核心：从存活的单位中随机抽取一个作为目标
     * 必须传入 Random 实例以保证回放一致性
     */
    public BattleUnit getRandomAliveUnit(Random random) {
        List<BattleUnit> aliveUnits = units.stream()
                .filter(u -> !u.isDead())
                .collect(Collectors.toList());

        if (aliveUnits.isEmpty()) {
            return null;
        }

        // 使用种子随机数选择一个目标
        int index = random.nextInt(aliveUnits.size());
        return aliveUnits.get(index);
    }

    /**
     * 检查整支部队是否能行动（比如是否全员被眩晕）
     */
    public boolean canAct() {
        return units.stream().anyMatch(BattleUnit::canAct);
    }

    /**
     * 统一结算所有单位的 Buff
     */
    public void tickBuffs() {
        for (BattleUnit unit : units) {
            if (!unit.isDead()) {
                unit.tickBuffs();
            }
        }
    }

    public void tickSkillCooldowns() {
        for (BattleUnit unit : units) {
            if (!unit.isDead()) {
                unit.tickSkillCooldowns();
            }
        }
    }

    // Getters
    public List<BattleUnit> getUnits() {
        return units;
    }

    public String getGroupName() {
        return groupName;
    }
}