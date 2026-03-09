package com.winter.modules.battle.model;

import java.util.List;

import com.winter.msg.BattleMsg;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BattleResult {
    private boolean win;               // 是否胜利 (true=攻击方赢, false=防守方赢)
    private long seed;                 // 本次战斗使用的随机种子
    private List<String> battleLogs;   // 战斗过程日志 (简易版)
    private BattleMsg.BattleRecord record; // Protobuf 战斗回放记录

    // 统计数据
    private int totalRounds;   // 总回合数
    private int attackerLoss;  // 攻击方损失兵力
    private int defenderLoss;  // 防守方损失兵力

    public BattleResult(boolean win, long seed, List<String> battleLogs) {
        this.win = win;
        this.seed = seed;
        this.battleLogs = battleLogs;
    }

    public BattleResult(boolean win, long seed, List<String> battleLogs, BattleMsg.BattleRecord record) {
        this.win = win;
        this.seed = seed;
        this.battleLogs = battleLogs;
        this.record = record;
    }

    /** 获取 Protobuf Action 列表 (兼容旧接口) */
    public List<BattleMsg.BattleAction> getActions() {
        return record != null ? record.getActionsList() : List.of();
    }

    /** 将整个战斗回放序列化为 Protobuf 字节数组，用于网络传输 */
    public byte[] toBytes() {
        return record != null ? record.toByteArray() : new byte[0];
    }

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