package com.winter.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.winter.common.model.PlayerModel;

public final class WorldManager {

    private WorldManager() {}

    // 在线玩家表：playerId -> PlayerModel
    public static final Map<Long, PlayerModel> onlinePlayers = new ConcurrentHashMap<>();
}