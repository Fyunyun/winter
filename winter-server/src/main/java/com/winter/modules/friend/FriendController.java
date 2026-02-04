package com.winter.modules.friend;

import com.winter.common.model.PlayerModel;
import com.winter.core.router.GameHandler;
import com.winter.modules.player.PlayerManager;
import com.winter.msg.MsgId.CmdId;
import com.winter.msg.PacketMsg.GamePacket;
import com.winter.msg.FriendMsg.ReqAddFriend;
import com.winter.msg.FriendMsg.RespAddFriend;
import com.winter.msg.FriendMsg.BrdFriendRequest;

import io.netty.channel.ChannelHandlerContext;

public class FriendController {

    FriendService friendService = new FriendService();

    // 请求添加好友
    @GameHandler(cmd = CmdId.REQ_ADD_FRIEND)
    public void addFriendRequest(ChannelHandlerContext ctx, PlayerModel player, byte[] data) {
        boolean successful = false;

        try {
            ReqAddFriend req = ReqAddFriend.parseFrom(data);
            successful = friendService.addFriendRequest(player, req.getTargetId());
            RespAddFriend resp;
            if (successful) {
                resp = RespAddFriend.newBuilder()
                        .setTargetId(req.getTargetId())
                        .setMessage("好友请求已发送")
                        .build();
                PlayerManager.sendToPlayer(req.getTargetId(),
                        BrdFriendRequest.newBuilder()
                                .setFromId(player.getPlayerId()).setFromName(player.getName())
                                .build(),
                        CmdId.BRD_FRIEND_REQUEST);
            } else {
                resp = RespAddFriend.newBuilder()
                        .setTargetId(req.getTargetId())
                        .setMessage("好友请求发送失败")
                        .build();
            }
            GamePacket packet = GamePacket.newBuilder()
                    .setCmd(CmdId.RESP_ADD_FRIEND)
                    .setContent(resp.toByteString())
                    .build();
            ctx.writeAndFlush(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
