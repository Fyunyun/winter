package com.winter.modules.move;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.winter.common.model.PlayerModel;
import com.winter.modules.player.PlayerManager;
import com.winter.modules.scene.AoiService;
import com.winter.msg.NotificationMsg;
import com.winter.msg.MsgId.CmdId;

import org.springframework.stereotype.Service;

@Service
public class MoveService {

    private final MoveDao moveDao;

    public MoveService(MoveDao moveDao) {
        this.moveDao = moveDao;
    }
    static final ExecutorService broadcastExecutor = Executors.newFixedThreadPool(4);

    public boolean movePlayer(PlayerModel player, float newX, float newY) {
        // TODO: 1. 检查移动合法性 (地图边界、障碍物等)
        if (newX < 0 || newY < 0) {
            return false; // 简单边界检查
        }

        // 2. 更新redis中玩家位置
        System.out.println("[移动] 玩家 " + player.getPlayerId() + " 移动到 (" + newX + ", " + newY + ")");
        moveDao.updatePlayerPosition(player, newX, newY);

        // 2. 更新 AOI 并获取附近玩家列表
        System.out.println("[AOI] 更新玩家 " + player.getPlayerId() + " 坐标到 AOI 系统");
        List<Long> updateAndGetNeighbors = AoiService.updateAndGetNeighbors(player.getPlayerId(), newX, newY);

        // 3. 广播给周围玩家
        System.out.println("[广播] 向 " + updateAndGetNeighbors.size() + " 个附近玩家广播移动信息");
        broadcastMove(player, newX, newY, updateAndGetNeighbors);

        return true;
    }

    public void broadcastMove(PlayerModel player, float newX, float newY, List<Long> neighborIds) {
        broadcastExecutor.submit(() -> {
            NotificationMsg.BrdPlayerMove brd = NotificationMsg.BrdPlayerMove.newBuilder()
                    .setPlayerId(player.getPlayerId())
                    .setX(newX)
                    .setY(newY)
                    .build();

            for (Long neighborId : neighborIds) {
                PlayerManager.sendToPlayer(neighborId, brd, CmdId.PUSH_PLAYER_POSITION);
            }
        });
    }
}
