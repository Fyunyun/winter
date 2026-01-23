package com.winter.modules.move;



import com.winter.common.model.PlayerModel;

public class MoveService {

    private MoveDao moveDao = new MoveDao();

    public boolean movePlayer(PlayerModel player, float newX, float newY) {
        // TODO: 1. 检查移动合法性 (地图边界、障碍物等)


        // 2. 更新玩家位置
        moveDao.updatePlayerPosition(player, newX, newY);
        return true;
    }

}
