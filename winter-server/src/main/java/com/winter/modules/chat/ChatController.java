package com.winter.modules.chat;

import com.winter.common.model.PlayerModel;
import com.winter.core.router.GameHandler;
import com.winter.msg.ChatMsg.ReqSendPrivateChat;
import com.winter.msg.MsgId.CmdId;

import io.netty.channel.ChannelHandlerContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    @Autowired
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GameHandler(cmd = CmdId.REQ_SEND_PRIVATE_CHAT)
    public void sendPrivateChat(ChannelHandlerContext ctx, PlayerModel player, byte[] data) {
        try {
            ReqSendPrivateChat req = ReqSendPrivateChat.parseFrom(data);

            // TODO: 这里后续可以调用 chatService 做私聊投递/持久化
            // chatService.sendPrivateChat(player, req);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
