package com.winter.modules.register;

import com.winter.common.model.PlayerModel;
import com.winter.core.router.GameHandler;
import com.winter.msg.ErrorMsg.ErrorCode;
import com.winter.msg.MsgId.CmdId;
import com.winter.msg.PacketMsg.GamePacket;
import com.winter.msg.RegisterMsg.ReqRegister;
import com.winter.msg.RegisterMsg.RespRegister;
import com.winter.msg.SuccessMsg.SuccessCode;

import io.netty.channel.ChannelHandlerContext;

public class RegisterController {

    RegisterService registerService = new RegisterService();

    @GameHandler(cmd = CmdId.REQ_REGISTER)
    public void register(ChannelHandlerContext ctx, PlayerModel player, byte[] data) {
        try {
            ReqRegister req = ReqRegister.parseFrom(data);
            String username = req.getUsername();
            String password = req.getPassword();
            int resultCode = registerService.register(username, password);
            String resultMsg;
            if (resultCode == SuccessCode.REGISTER_SUCCESS.getNumber()) {
                resultMsg = "注册成功";
            } else if (resultCode == ErrorCode.REGISTER_USERNAME_OR_PASSWORD_EMPTY.getNumber()) {
                resultMsg = "用户名或密码不能为空";
            } else if (resultCode == ErrorCode.REGISTER_USER_ALREADY_EXISTS.getNumber()) {
                resultMsg = "用户名已存在";
            } else {
                resultMsg = "注册失败，未知错误";
            }

            RespRegister.Builder resp = RespRegister.newBuilder()
                    .setCode(resultCode)
                    .setMsg(resultMsg);

            GamePacket.Builder packetBuilder = GamePacket.newBuilder()
                    .setCmd(CmdId.RESP_REGISTER)
                    .setContent(resp.build().toByteString());
            ctx.writeAndFlush(packetBuilder.build());

        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }
}
