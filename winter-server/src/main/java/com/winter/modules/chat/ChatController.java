package com.winter.modules.chat;

import com.winter.common.model.PlayerModel;
import com.winter.core.router.GameHandler;
import com.winter.msg.ChatMsg.ReqSendPrivateChat;
import com.winter.msg.MsgId.CmdId;
import com.winter.msg.ChatMsg.ReqSendGroupChat;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    @Autowired
    private ChatService chatService;

    @GameHandler(cmd = CmdId.REQ_SEND_PRIVATE_CHAT)
    public void sendPrivateChat(ChannelHandlerContext ctx, PlayerModel player, byte[] data) {
        try {
            ReqSendPrivateChat req = ReqSendPrivateChat.parseFrom(data);

            chatService.sendPrivateChat(player, req.getTargetId(), req.getContent(), req.getMsgType());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @GameHandler(cmd = CmdId.REQ_BROADCAST_CHAT)
    public void broadcastChat(ChannelHandlerContext ctx, PlayerModel player, byte[] data) {
        try {
            ReqSendGroupChat req = ReqSendGroupChat.parseFrom(data);

            chatService.broadcastToAll(player, req.getContent());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
