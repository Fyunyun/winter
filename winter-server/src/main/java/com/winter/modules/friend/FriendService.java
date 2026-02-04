package com.winter.modules.friend;

import com.winter.common.model.PlayerModel;

public class FriendService {

    FriendDao friendDao = new FriendDao();

    public boolean addFriendRequest(PlayerModel player, Long friendId) {
        if (player.getPlayerId() == friendId) {
            System.out.println("不能添加自己为好友，playerId=" + player.getPlayerId());
            return false;
        }

        if (friendDao.isFriend(player.getPlayerId(), friendId)) {
            // 已经是好友关系
            System.out.println("已经是好友关系，playerId=" + player.getPlayerId() + ", friendId=" + friendId);
            return false;
        }
        boolean friendRequest = friendDao.addFriendRequest(player.getPlayerId(), friendId);
        return friendRequest;
    }

    public boolean handleFriendRequest(PlayerModel player, Long targetId, boolean accept) {
        if (!friendDao.hasRequest(player.getPlayerId(), targetId)) {
            return false;
        }
        if (accept) {
            return friendDao.acceptFriendRequest(targetId, player.getPlayerId());
        } else {
            return friendDao.rejectFriendRequest(targetId, player.getPlayerId());
        }
    }
}
