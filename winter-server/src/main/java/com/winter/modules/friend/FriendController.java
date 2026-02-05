package com.winter.modules.friend;

import com.winter.common.model.PlayerModel;
import com.winter.core.router.GameHandler;
import com.winter.modules.player.PlayerManager;
import com.winter.msg.MsgId.CmdId;
import com.winter.msg.PacketMsg.GamePacket;
import com.winter.msg.FriendMsg.ReqAddFriend;
import com.winter.msg.FriendMsg.RespAddFriend;
import com.winter.msg.FriendMsg.RespFriendList;
import com.winter.msg.FriendMsg.RespHandleFriend;
import com.winter.msg.FriendMsg.BrdFriendRequest;
import com.winter.msg.FriendMsg.ReqHandleFriend;

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

    // 处理好友请求
    @GameHandler(cmd = CmdId.REQ_HANDLE_FRIEND)
    public void handleFriendReq(ChannelHandlerContext ctx, PlayerModel player, byte[] data) {
        try {
            ReqHandleFriend req = ReqHandleFriend.parseFrom(data);
            boolean result = friendService.handleFriendRequest(player, req.getTargetId(), req.getAgree());
            // 反馈结果
            RespAddFriend resp = RespAddFriend.newBuilder()
                    .setTargetId(req.getTargetId())
                    .setMessage(result ? "好友请求处理成功" : "好友请求处理失败")
                    .build();
            GamePacket packet = GamePacket.newBuilder()
                    .setCmd(CmdId.RESP_HANDLE_FRIEND)
                    .setContent(resp.toByteString())
                    .build();
            ctx.writeAndFlush(packet);

            PlayerManager.sendToPlayer(req.getTargetId(),
                    req.getAgree() ? RespHandleFriend.newBuilder()
                            .setTargetId(player.getPlayerId())
                            .setMessage("你的好友请求已被接受")
                            .build()
                            : RespHandleFriend.newBuilder()
                                    .setTargetId(player.getPlayerId())
                                    .setMessage("你的好友请求已被拒绝")
                                    .build(),
                    CmdId.RESP_HANDLE_FRIEND);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 获取好友列表
    @GameHandler(cmd = CmdId.REQ_GET_FRIEND_LIST)
    public void getFriendList(ChannelHandlerContext ctx, PlayerModel player, byte[] data) {
        try {
            RespFriendList friendList = friendService.getFriendList(player.getPlayerId());

            GamePacket packet = GamePacket.newBuilder()
                    .setCmd(CmdId.RESP_FRIEND_LIST)
                    .setContent(friendList.toByteString())
                    .build();
            ctx.writeAndFlush(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
