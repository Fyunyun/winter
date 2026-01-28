package com.winter.modules.collect;

import com.winter.common.model.PlayerModel;
import com.winter.core.db.DataService;

public class CollectDao {

    public boolean collectCoal(PlayerModel player, int amount) {
        if (player != null) {
            player.setCoal(player.getCoal() + amount);
            DataService.updateResourceInRedis(player);
            return true;
        } else {
            return false;
        }

    }

    public boolean collectWood(PlayerModel player, int amount) {
        if (player != null) {
            player.setWood(player.getWood() + amount);
            DataService.updateResourceInRedis(player);
            return true;
        } else {
            return false;
        }
    }

    public boolean collectFood(PlayerModel player, int amount) {
        if (player != null) {
            player.setFood(player.getFood() + amount);
            DataService.updateResourceInRedis(player);
            return true;
        } else {
            return false;
        }
    }

}
