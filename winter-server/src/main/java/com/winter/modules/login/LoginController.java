package com.winter.modules.login;

import com.winter.common.model.PlayerModel;
import com.winter.core.router.GameHandler;
import com.winter.core.util.SessionUtil;
import com.winter.msg.AuthMsg.RespLogin;
import com.winter.msg.MsgId.CmdId;
import com.winter.msg.PacketMsg.GamePacket;
import com.winter.msg.SuccessMsg.SuccessCode;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;

public class LoginController {

    LoginService loginService = new LoginService();

    @GameHandler(cmd = CmdId.REQ_LOGIN)
    public void login(ChannelHandlerContext ctx, PlayerModel player, byte[] data) {

        PlayerModel result = loginService.handleLogin(ctx, player, data);
        if (result != null) {
            SessionUtil.bindPlayerId(ctx.channel(), result.getPlayerId());
            ctx.channel().attr(AttributeKey.valueOf("PLAYER")).set(result);

            RespLogin resp = RespLogin.newBuilder()
                    .setCode(SuccessCode.LOGIN_SUCCESS.getNumber())
                    .setMsg("登录成功")
                    .setPlayerid(result.getPlayerId())
                    .build();
            GamePacket packet = GamePacket.newBuilder()
                    .setCmd(CmdId.RESP_LOGIN)
                    .setContent(resp.toByteString())
                    .build();
            ctx.writeAndFlush(packet);
        }
    }
}
