package com.winter.modules.scene;

import com.winter.core.db.DbManager;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.resps.GeoRadiusResponse;
import java.util.ArrayList;
import java.util.List;
import redis.clients.jedis.args.GeoUnit;

public class AoiService {

    private static final String GEO_KEY = "world:map:pos";
    private static final double AOI_RADIUS = 500.0; // 视野半径 500 千米

    /**
     * 更新我的坐标，并寻找周围的人
     */
    public static final List<Long> updateAndGetNeighbors(long myId, float x, float y) {
        List<Long> neighborIds = new ArrayList<>();

        try (Jedis redis = DbManager.getJedis()) {
            // 1. 更新我的位置到 Redis GEO
            // GeoAdd: 将 member(myId) 加到 key 中，坐标为 x, y
            redis.geoadd(GEO_KEY, x, y, String.valueOf(myId));

            // 2. 搜索我周围 500 千米内的人
            // GeoRadius: 返回成员列表
            List<GeoRadiusResponse> responses = redis.georadius(
                    GEO_KEY,
                    x,
                    y,
                    AOI_RADIUS,
                    GeoUnit.KM, // 单位：千米
                    GeoRadiusParam.geoRadiusParam().withDist() // 可以顺便返回距离
            );

            // 3. 过滤结果
            for (GeoRadiusResponse resp : responses) {
                String member = resp.getMemberByString();
                long neighborId = Long.parseLong(member);

                // 排除我自己
                if (neighborId != myId) {
                    neighborIds.add(neighborId);
                }
            }
        }
        return neighborIds;
    }

    /**
     * 玩家下线移除坐标
     */
    public static final void removePlayer(long playerId) {
        try (Jedis redis = DbManager.getJedis()) {
            redis.zrem(GEO_KEY, String.valueOf(playerId));
        }
    }
}