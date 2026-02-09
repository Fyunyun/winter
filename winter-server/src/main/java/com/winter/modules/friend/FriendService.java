package com.winter.modules.friend;

import java.util.List;

import com.winter.common.model.PlayerModel;
import com.winter.core.db.DataService;
import com.winter.modules.friend.Entry.FriendEntry;
import com.winter.modules.player.PlayerManager;
import com.winter.msg.FriendMsg.RespFriendList;
import com.winter.msg.FriendMsg.FriendInfo;

import org.springframework.stereotype.Service;

@Service
public class FriendService {

    private final FriendDao friendDao;

    public FriendService(FriendDao friendDao) {
        this.friendDao = friendDao;
    }

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

    public RespFriendList getFriendList(Long myId) {
        List<FriendEntry> friendList = friendDao.getFriendList(myId);

        RespFriendList.Builder resp = RespFriendList.newBuilder();

        for (FriendEntry friendEntry : friendList) {
            PlayerModel friendModel = DataService.loadPlayerFromRedis(friendEntry.getFriendId());
            boolean online = PlayerManager.isOnline(friendEntry.getFriendId());

            FriendInfo friendInfo = FriendInfo.newBuilder()
                    .setFriendId(friendEntry.getFriendId())
                    .setFriendName(friendModel != null ? friendModel.getName() : "未知")
                    .setFriendLevel(friendModel != null ? friendModel.getLevel() : 0)
                    .setIsOnline(online)
                    .setStatus(friendEntry.getStatus())
                    .build();
            resp.addFriends(friendInfo);      
        }
        return resp.build();
    }
}
