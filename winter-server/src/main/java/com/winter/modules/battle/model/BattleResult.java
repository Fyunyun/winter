package com.winter.modules.battle.model;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor
public class BattleResult {
    private boolean win; // 是否胜利 (true=攻击方赢, false=防守方赢)
    private long seed; // 本次战斗使用的随机种子
    private List<String> battleLogs; // 战斗过程日志 (简易版)
    private List<BattleAction> actions = new ArrayList<>(); // 进阶版：Protobuf定义的行为序列，用于真回放

    // 统计数据
    private int totalRounds; // 总回合数
    private int attackerLoss; // 攻击方损失兵力
    private int defenderLoss; // 防守方损失兵力

    public BattleResult(boolean win, long seed, List<String> battleLogs) {
        this.win = win;
        this.seed = seed;
        this.battleLogs = battleLogs;
        this.actions = new ArrayList<>();
    }

    public BattleResult(boolean win, long seed, List<String> battleLogs, List<BattleAction> actions) {
        this.win = win;
        this.seed = seed;
        this.battleLogs = battleLogs;
        this.actions = (actions != null) ? actions : new ArrayList<>();
    }

    public void addAction(BattleAction action) {
        if (this.actions == null) {
            this.actions = new ArrayList<>();
        }
        this.actions.add(action);
    }

    public List<BattleAction> getActions() {
        return actions;
    }

    // Getters 和 Setters
    public boolean isWin() {
        return win;
    }

    public long getSeed() {
        return seed;
    }

    public List<String> getBattleLogs() {
        return battleLogs;
    }

    public void setStats(int rounds, int atkLoss, int defLoss) {
        this.totalRounds = rounds;
        this.attackerLoss = atkLoss;
        this.defenderLoss = defLoss;
    }

}