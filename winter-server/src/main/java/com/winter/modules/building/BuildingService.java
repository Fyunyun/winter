package com.winter.modules.building;

import com.alibaba.fastjson.JSON;

import redis.clients.jedis.Jedis;

import com.winter.common.model.BuildingModel;
import com.winter.common.model.PlayerModel;
import com.winter.core.db.DataService;
import com.winter.core.db.DbManager;

public class BuildingService {

    private static final BuildingDao buildingDao = new BuildingDao();

    // --- 1. 获取某个建筑 (先查 Redis，没有再查 MySQL) ---
    public BuildingModel getBuilding(long playerId, int type) {
        String key = "p:build:" + playerId;
        try (Jedis redis = DbManager.getJedis()) {
            BuildingModel dbModel;

            // A. 查 Redis
            String json = redis.hget(key, String.valueOf(type));
            if (json != null) {
                dbModel = JSON.parseObject(json, BuildingModel.class);
            } else {
                // B. Redis 没有，查 MySQL 并回填
                dbModel = buildingDao.loadFromMysql(playerId, type);
                if (dbModel != null) {
                    redis.hset(key, String.valueOf(type), JSON.toJSONString(dbModel));
                } else {
                    // 如果数据库也没有，说明玩家还没建这个建筑，创建一个默认的 0 级建筑
                    dbModel = new BuildingModel(type, 0);
                    redis.hset(key, String.valueOf(type), JSON.toJSONString(dbModel));
                }
            }

            // C. 如果建筑正在升级，检查是否升级完成
            if (dbModel.getStatus() == 1 && System.currentTimeMillis() >= dbModel.getFinishTime()) {
                dbModel.setLevel(dbModel.getLevel() + 1);
                dbModel.setStatus(0);
                dbModel.setFinishTime(0);

                buildingDao.saveBuildingToRedis(playerId, dbModel);
                buildingDao.saveBuildingToMysql(playerId, dbModel);

            }
            return dbModel;
        }
    }

    // --- 2. 升级建筑 (核心事务逻辑) ---
    public Integer upgradeBuilding(PlayerModel player, int type) {
        // A. 获取建筑数据
        BuildingModel build = getBuilding(player.getPlayerId(), type);

        // B. 各种检查
        if (build.isUpgrading()) {
            return -1;
        }

        // C. 检查资源 (假设升级需要：等级 * 100 木头)
        long costWood = (build.getLevel() + 1) * 100L;
        if (player.getWood() < costWood) {
            return -2;
        }

        // D. 【执行扣费】 (更新 Redis 中的玩家资源)
        player.setWood(player.getWood() - costWood);
        DataService.updateResourceInRedis(player); // 之前写的更新资源方法

        // E. 【执行升级】 (更新 Redis 中的建筑状态
        build.setStatus(1); // 标记为升级中
        // 假设升级耗时 durationMs = (等级 + 1) * 10000 毫秒
        long durationMs = (build.getLevel() + 1) * 10000L;
        build.setFinishTime(System.currentTimeMillis() + durationMs);

        // F. 保存建筑数据到 Redis
        buildingDao.saveBuildingToRedis(player.getPlayerId(), build);

        // G. 异步/同步保存到 MySQL (防止回档)
        // 在实际项目中，这里通常是丢给 Log线程或者定时任务
        buildingDao.saveBuildingToMysql(player.getPlayerId(), build);
        return 0;
    }

    // --- 4. 创建新建筑 ---
    public boolean createBuilding(long playerId, int type) {
        BuildingModel building = new BuildingModel(type, 0);
        buildingDao.saveBuildingToRedis(playerId, building);
        buildingDao.saveBuildingToMysql(playerId, building);
        return true;
    }
}