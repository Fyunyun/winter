package com.winter.modules.collect;

import com.winter.common.model.PlayerModel;

import org.springframework.stereotype.Service;

@Service
public class CollectService {

    private final CollectDao collectDao;

    public CollectService(CollectDao collectDao) {
        this.collectDao = collectDao;
    }

    public boolean collectCoal(PlayerModel player, int amount) {
        // 处理采集煤炭的逻辑
        return collectDao.collectCoal(player, amount);
    }
    public boolean collectWood(PlayerModel player, int amount) {
        // 处理采集木材的逻辑
        System.out.println("Collecting wood...");
        return collectDao.collectWood(player, amount);
    }
    public boolean collectFood(PlayerModel player, int amount) {
        // 处理采集食物的逻辑
        System.out.println("Collecting food...");
        return collectDao.collectFood(player, amount);
    }

}
