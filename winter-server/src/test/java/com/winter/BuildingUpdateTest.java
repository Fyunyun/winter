package com.winter;

import com.winter.db.DataService;
import com.winter.model.BuildingModel;
import com.winter.model.PlayerModel;
import com.winter.service.BuildingService;

public class BuildingUpdateTest {
    public static void main(String[] args) throws InterruptedException {
        long pid = 99999; 
        PlayerModel player = DataService.loadPlayer(pid);
        
        // 1. 初始化资源 (确保有足够木头)
        player.setWood(10000); 
        DataService.updateResourceInRedis(player);

        System.out.println("=== 1. 初始状态 ===");
        BuildingModel furnace = BuildingService.getBuilding(pid, 1);
        // 为了方便测试，先把等级重置为 1，状态重置为 0
        furnace.setLevel(1); furnace.setStatus(0); furnace.setFinishTime(0);
        // 这是一个 hack 方法，实际项目写专门的 reset
        // 这里我们简单粗暴直接写库覆盖一下初始状态
        try (var redis = com.winter.db.DbManager.getJedis()) {
             redis.hset("p:build:"+pid, "1", com.alibaba.fastjson.JSON.toJSONString(furnace));
        }

        System.out.println("当前等级: " + furnace.getLevel() + ", 状态: " + furnace.getStatus());

        System.out.println("\n=== 2. 点击升级 ===");
        // 注意：这里我们为了测试，去 BuildingService 把升级耗时改成 3000 毫秒 (3秒)，别用 60秒了，太久
        // 请去 BuildingService.upgradeBuilding 方法里把 durationMs 临时改成 3000L
        String result = BuildingService.upgradeBuilding(player, 1);
        System.out.println("操作结果: " + result);

        System.out.println("\n=== 3. 立即尝试结算 (应该失败) ===");
        boolean success = BuildingService.completeBuildingUpgrade(pid, 1);
        System.out.println("结算结果: " + (success ? "成功" : "失败 (时间未到)"));

        System.out.println("\n=== 4. 模拟等待 3.5 秒 ===");
        Thread.sleep(3500); 

        System.out.println("\n=== 5. 再次尝试结算 (应该成功) ===");
        success = BuildingService.completeBuildingUpgrade(pid, 1);
        System.out.println("结算结果: " + (success ? "成功" : "失败"));

        System.out.println("\n=== 6. 最终验证 ===");
        BuildingModel finalBuild = BuildingService.getBuilding(pid, 1);
        System.out.println("最终等级: " + finalBuild.getLevel());
        System.out.println("最终状态: " + finalBuild.getStatus() + " (0代表闲置)");
    }
}
