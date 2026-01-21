package com.winter.model;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

@Data
public class BuildingModel {

    private long playerId;  // 所属玩家ID
    private int buildingType;       // 建筑类型 (1:熔炉, 2:兵营, 3:伐木场)
    private int level;      // 等级
    private int status;     // 0:闲置, 1:升级中
    private long finishTime;// 结束时间戳

    // 无参构造 (FastJSON 反序列化需要)
    public BuildingModel() {}

    public BuildingModel(int buildingType, int level) {
        this.buildingType = buildingType;
        this.level = level;
        this.status = 0;
        this.finishTime = 0;
    }

    // --- 核心业务逻辑 ---
    
    /**
     * 检查建筑是否正在升级中
     * @return true=正在升级
     */
    @JSONField(serialize = false) // 不存入 JSON，实时计算
    public boolean isUpgrading() {
        if (status == 1) {
            long now = System.currentTimeMillis();
            if (now >= finishTime) {
                return false; // 时间到了，其实已经算完成了（等待结算）
            }
            return true; // 时间还没到
        }
        return false;
    }
}