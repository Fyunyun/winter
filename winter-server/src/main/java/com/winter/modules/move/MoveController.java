package com.winter.modules.move;

import com.google.protobuf.InvalidProtocolBufferException;
import com.winter.common.model.PlayerModel;
import com.winter.core.router.GameHandler;
import com.winter.msg.MsgId.CmdId;
import com.winter.msg.PacketMsg.GamePacket;
import com.winter.msg.MoveMsg.ReqMove;
import com.winter.msg.MoveMsg.RespMove;

import io.netty.channel.ChannelHandlerContext;

import org.springframework.stereotype.Component;

@Component
public class MoveController {

    private final MoveService moveService;

    public MoveController(MoveService moveService) {
        this.moveService = moveService;
    }

    @GameHandler(cmd = CmdId.REQ_MOVE)
    public void handleMoveReq(ChannelHandlerContext ctx, PlayerModel player, byte[] data) {
        try {
            ReqMove req = ReqMove.parseFrom(data);
            Boolean movePlayer = moveService.movePlayer(player, req.getX(), req.getY());
            
            String moveSuccess = movePlayer ? "Move successful" : "Move failed";
            
            RespMove resp = RespMove.newBuilder()
                    .setX(req.getX())
                    .setY(req.getY())
                    .setMsg(moveSuccess)
                    .build();
            GamePacket packet = GamePacket.newBuilder()
                    .setCmd(CmdId.RESP_MOVE)
                    .setContent(resp.toByteString())
                    .build();
            ctx.writeAndFlush(packet);
            
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }
}
