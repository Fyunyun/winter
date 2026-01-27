package com.winter.core.net.handler;// 定义包名

import com.winter.msg.AuthMsg.RespLogin;
import com.winter.msg.MsgId.CmdId;
import com.winter.msg.ErrorMsg.ErrorCode;
import com.winter.msg.PacketMsg.GamePacket;
import com.winter.core.util.SessionUtil;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

// 定义一个继承自ChannelInboundHandlerAdapter的类，用于处理入站消息
public class AuthenticationHandler extends SimpleChannelInboundHandler<GamePacket> {
    // 重写channelRead方法，用于处理接收到的消息
    @Override
    public void channelRead0(ChannelHandlerContext ctx, GamePacket msg) throws Exception {
        if (msg.getCmd() != CmdId.REQ_LOGIN) {
            if (!SessionUtil.hasLogin(ctx.channel())) {

                RespLogin loginMsg = RespLogin.newBuilder()
                        .setCode(ErrorCode.LOGIN_USER_NOT_LOGGED_IN.getNumber())
                        .setMsg("账号未登录，请先登录")
                        .build();

                GamePacket resp = GamePacket.newBuilder()
                        .setCmd(CmdId.RESP_LOGIN)
                        .setContent(loginMsg.toByteString())
                        .build();
                ctx.writeAndFlush(resp);
                return;
            }
            ctx.fireChannelRead(msg);
            return;
        }
    }
}