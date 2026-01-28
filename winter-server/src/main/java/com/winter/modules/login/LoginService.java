package com.winter.modules.login;

import com.winter.common.model.PlayerModel;
import com.winter.core.db.DataService;
import com.winter.msg.AuthMsg.ReqLogin;
import com.winter.msg.AuthMsg.RespLogin;
import com.winter.msg.ErrorMsg.ErrorCode;
import com.winter.msg.MsgId.CmdId;
import com.winter.msg.PacketMsg.GamePacket;

import io.netty.channel.ChannelHandlerContext;

public class LoginService {

    LoginDao loginDao = new LoginDao();

    /**
     * 统一的登录入口
     * 
     * @return 登录成功返回 PlayerModel，失败返回 null (或者抛出异常)
     */
    public PlayerModel handleLogin(ChannelHandlerContext ctx, PlayerModel player, byte[] data) {

        try {
            ReqLogin loginReq = ReqLogin.parseFrom(data);
            String username = loginReq.getUsername();
            String password = loginReq.getPassword();
            // 步骤 1: 验证身份 (这一步走 MySQL)
            long pid = loginDao.LOGIN_VERIFY(username);

            if (pid <= 0) {
                RespLogin loginMsg = RespLogin.newBuilder()
                        .setCode(ErrorCode.LOGIN_USER_NOT_EXISTS.getNumber())
                        .setMsg("玩家不存在,请注册新账号")
                        .build();

                GamePacket resp = GamePacket.newBuilder()
                        .setCmd(CmdId.RESP_LOGIN)
                        .setContent(loginMsg.toByteString())
                        .build();

                ctx.writeAndFlush(resp);
                return null;
            }

            boolean passwordValid = loginDao.VERIFY_PASSWORD(username, password);
            if (!passwordValid) {
                RespLogin loginMsg = RespLogin.newBuilder()
                        .setCode(ErrorCode.LOGIN_PWD_USERNAME_INCORRECT.getNumber())
                        .setMsg("登录失败，账号或密码错误")
                        .build();

                GamePacket resp = GamePacket.newBuilder()
                        .setCmd(CmdId.RESP_LOGIN)
                        .setContent(loginMsg.toByteString())
                        .build();

                ctx.writeAndFlush(resp);
                return null;
            }

            // 步骤 2: (可选) 检查账号是否被封号
            // if (BanManager.isBanned(pid)) { ... }

            // 步骤 3: 加载数据 (这一步优先走 Redis)
            PlayerModel loadplayer = loginDao.loadPlayer(pid);

            if (loadplayer == null) {
                System.out.println("严重错误：账号存在但角色数据丢失！PID: " + pid);
                RespLogin loginMsg = RespLogin.newBuilder()
                    .setCode(ErrorCode.ERROR_UNKNOWN.getNumber())
                    .setMsg("登录失败：玩家数据不存在")
                    .build();

                GamePacket resp = GamePacket.newBuilder()
                    .setCmd(CmdId.RESP_LOGIN)
                    .setContent(loginMsg.toByteString())
                    .build();

                ctx.writeAndFlush(resp);
                return null;
            }

            // 每次登录都刷新一下缓存中的数据
            DataService.updateResourceInRedis(loadplayer);

            return loadplayer;

        } catch (Exception e) {
            e.printStackTrace();
            try {
                RespLogin loginMsg = RespLogin.newBuilder()
                        .setCode(ErrorCode.ERROR_UNKNOWN.getNumber())
                        .setMsg("登录失败：服务异常")
                        .build();

                GamePacket resp = GamePacket.newBuilder()
                        .setCmd(CmdId.RESP_LOGIN)
                        .setContent(loginMsg.toByteString())
                        .build();

                ctx.writeAndFlush(resp);
            } catch (Exception inner) {
                inner.printStackTrace();
            }
        }
        return null;
    }
}