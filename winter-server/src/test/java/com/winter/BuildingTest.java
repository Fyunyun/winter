// package com.winter;

// import com.winter.common.model.BuildingModel;
// import com.winter.common.model.PlayerModel;
// import com.winter.core.db.DataService;
// import com.winter.modules.building.BuildingService;

// public class BuildingTest {
//     public static void main(String[] args) throws InterruptedException {
//         // 1. 模拟登录
//         long pid = 99999; 
//         PlayerModel player = DataService.loadPlayer(pid);
        
//         // 给你充点钱，防止穷得没法测试
//         player.setWood(5000); 
//         DataService.updateResourceInRedis(player);

//         System.out.println("=== 开始建筑测试 ===");
        
//         // 2. 查看当前熔炉状态 (Type = 1)
//         BuildingModel furnace = BuildingService.getBuilding(pid, 1);
//         System.out.println("当前熔炉等级: " + furnace.getLevel());

//         // 3. 尝试升级
//         System.out.println("\n[操作] 点击升级按钮...");
//         Integer result = BuildingService.upgradeBuilding(player, 1);
//         System.out.println("服务器返回: " + result);

//         // 4. 再次查看状态
//         furnace = BuildingService.getBuilding(pid, 1);
//         System.out.println("当前状态: " + (furnace.isUpgrading() ? "升级中..." : "空闲"));
//         System.out.println("剩余时间: " + (furnace.getFinishTime() - System.currentTimeMillis()) / 1000 + "秒");

//         // 5. 模拟: 重复点击升级 (应该失败)
//         System.out.println("\n[操作] 再次点击升级...");
//         System.out.println("服务器返回: " + BuildingService.upgradeBuilding(player, 1));
        
//         // 6. 验证资源是否扣除
//         PlayerModel pAfter = DataService.loadPlayer(pid);
//         System.out.println("\n剩余木材: " + pAfter.getWood());
//     }
// }