package com.winter.modules.move;

import java.util.concurrent.CompletableFuture;
import com.winter.common.model.PlayerModel;
import com.winter.core.db.DataService;
import com.winter.core.db.DbManager;
import redis.clients.jedis.Jedis;

public class MoveDao {

    public boolean updatePlayerPosition(PlayerModel player, float x, float y) {

        player.setX(x);
        player.setY(y);

        CompletableFuture.runAsync(() -> {
            try (Jedis redis = DbManager.getJedis()) {
                // 更新缓存
                DataService.updateResourceInRedis(player);

                // 更新位置索引
                redis.geoadd("world:map:pos", x, y, String.valueOf(player.getPlayerId()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

         //TODO 广播给周围的人 (AOI)
        return true;

    }
}
