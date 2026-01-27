package com.winter.modules.login;

import com.winter.common.model.PlayerModel;
import com.winter.core.router.GameHandler;
import com.winter.core.util.SessionUtil;
import com.winter.msg.MsgId.CmdId;

import io.netty.channel.ChannelHandlerContext;

public class LoginController {

    LoginService loginService = new LoginService();

    @GameHandler(cmd = CmdId.REQ_LOGIN)
    public void login(ChannelHandlerContext ctx, PlayerModel player, byte[] data) {

        PlayerModel result = loginService.handleLogin(ctx, player, data);
        if (result == null) {
            SessionUtil.bindPlayerId(ctx.channel(), player.getPlayerId());
        }

    }
}
