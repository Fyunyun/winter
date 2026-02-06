package com.winter.modules.login;

import com.winter.common.model.PlayerModel;
import com.winter.core.db.DbManager;
import com.winter.core.router.GameHandler;
import com.winter.core.util.SessionUtil;
import com.winter.modules.player.PlayerManager;
import com.winter.msg.AuthMsg.RespLogin;
import com.winter.msg.MsgId.CmdId;

import com.winter.msg.PacketMsg.GamePacket;
import com.winter.msg.SuccessMsg.SuccessCode;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import redis.clients.jedis.Jedis;

import org.springframework.stereotype.Component;

@Component
public class LoginController {

    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @GameHandler(cmd = CmdId.REQ_LOGIN)
    public void login(ChannelHandlerContext ctx, PlayerModel player, byte[] data) {

        PlayerModel result = loginService.handleLogin(ctx, player, data);
        if (result != null) {
            SessionUtil.bindPlayerId(ctx.channel(), result.getPlayerId());
            ctx.channel().attr(AttributeKey.valueOf("PLAYER")).set(result);

            // 将玩家加入在线列表   
            PlayerManager.addPlayer(result, ctx.channel());

            try{
                Jedis redis = DbManager.getJedis();
            redis.geoadd("world:map:pos", result.getX(), result.getY(), String.valueOf(result.getPlayerId()));
            } catch(Exception e){
                e.printStackTrace();
            }
            

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
