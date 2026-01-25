package com.winter.core.net.handler;// 定义包名

import com.winter.common.model.PlayerModel;
import com.winter.msg.AuthMsg.ReqLogin;
import com.winter.msg.AuthMsg.RespLogin;
import com.winter.msg.MsgId.CmdId;
import com.winter.msg.ErrorMsg.ErrorCode;
import com.winter.msg.PacketMsg.GamePacket;
import com.winter.modules.auth.LoginService;
import com.winter.core.util.SessionUtil;
import io.netty.channel.ChannelFutureListener;
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

        ReqLogin loginReq = ReqLogin.parseFrom(msg.getContent());
        String username = loginReq.getUsername();
        String password = loginReq.getPassword();
        PlayerModel player = LoginService.handleLogin(username, password);
        if (player != null) {
            RespLogin loginMsg = RespLogin.newBuilder()
                    .setCode(ErrorCode.LOGIN_USER_NOT_EXISTS.getNumber())
                    .setMsg("玩家不存在")
                    .build();

            GamePacket resp = GamePacket.newBuilder()
                    .setCmd(CmdId.RESP_LOGIN)
                    .setContent(loginMsg.toByteString())
                    .build();

            ctx.writeAndFlush(resp);

            SessionUtil.bindPlayerId(ctx.channel(), player.getPlayerId());
            
            // 可选：登录成功后移除认证 handler，避免每条消息都判断一次
            ctx.pipeline().remove(this);
        } else {
            RespLogin loginMsg = RespLogin.newBuilder()
                    .setCode(ErrorCode.LOGIN_PWD_USERNAME_INCORRECT.getNumber())
                    .setMsg("登录失败，账号或密码错误")
                    .build();

            GamePacket resp = GamePacket.newBuilder()
                    .setCmd(CmdId.RESP_LOGIN)
                    .setContent(loginMsg.toByteString())
                    .build();
            ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
            return;
        }
    }
}