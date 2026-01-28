package com.winter.modules.collect;

import com.winter.common.model.PlayerModel;
import com.winter.core.router.GameHandler;
import com.winter.msg.MsgId.CmdId;
import com.winter.msg.PacketMsg.GamePacket;
import com.winter.msg.CollectMsg.ReqCollect;
import com.winter.msg.CollectMsg.RespCollect;

import io.netty.channel.ChannelHandlerContext;

public class CollectController {

    CollectService collectService = new CollectService();

    @GameHandler(cmd = { CmdId.REQ_COLLECT_COAL, CmdId.REQ_COLLECT_WOOD, CmdId.REQ_COLLECT_FOOD })
    public void collectResource(ChannelHandlerContext ctx, PlayerModel player, byte[] data, CmdId cmdId) {
        try {
            boolean success = false;
            ReqCollect req = ReqCollect.parseFrom(data);
            int resourceType = req.getResourceType();
            int amount = req.getAmount();
            switch (resourceType) {
                case 1:
                    success = collectService.collectCoal(player, amount);
                    cmdId = CmdId.RESP_COLLECT_COAL;
                    break;
                case 2:
                    success = collectService.collectWood(player, amount);
                    cmdId = CmdId.RESP_COLLECT_WOOD;
                    break;
                case 3:
                    success = collectService.collectFood(player, amount);
                    cmdId = CmdId.RESP_COLLECT_FOOD;
                    break;
                default:
                    break;
            }
            RespCollect.Builder respBuilder = RespCollect.newBuilder();
            if (success) {
                respBuilder
                        .setCode(1)
                        .setPlayerid(player.getPlayerId())
                        .setMsg("Resource collected successfully")
                        .build();
            } else {
                respBuilder
                        .setCode(2)
                        .setPlayerid(player.getPlayerId())
                        .setMsg("Resource collected failed")
                        .build();
            }
            GamePacket packet = GamePacket.newBuilder()
                    .setCmd(cmdId)
                    .setContent(respBuilder.build().toByteString())
                    .build();
            ctx.writeAndFlush(packet);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
