package com.winter.modules.building;

import com.google.protobuf.InvalidProtocolBufferException;
import com.winter.common.model.BuildingModel;
import com.winter.common.model.PlayerModel;
import com.winter.core.router.GameHandler;
import com.winter.msg.BuildingMsg.ReqBuildUpgrade;
import com.winter.msg.BuildingMsg.RespBuildUpgrade;
import com.winter.msg.BuildingMsg.ReqBuildCreate;
import com.winter.msg.BuildingMsg.RespBuildCreate;
import com.winter.msg.MsgId.CmdId;
import com.winter.msg.PacketMsg.GamePacket;

import io.netty.channel.ChannelHandlerContext;

public class BuildingController {

    private final BuildingService buildingService = new BuildingService();

    @GameHandler(cmd = CmdId.REQ_BUILDING_UPGRADE)
    public void upgrade(ChannelHandlerContext ctx, PlayerModel player, byte[] data) {
        try {
            // 1. 反序列化具体的业务包 (从 bytes 变成 Object)
            ReqBuildUpgrade req = ReqBuildUpgrade.parseFrom(data);
            
            System.out.println("收到升级请求，建筑类型: " + req.getBuildingType());

            // 2. 调用业务逻辑 (复用你之前的 BuildingService)
            Integer result = buildingService.upgradeBuilding(player, req.getBuildingType());
            
            String msg = "";
            switch (result) {
                case 0: msg = "升级成功"; break;
                case -1: msg = "正在升级中"; break;
                case -2: msg = "资源不足"; break;
            }
           
            // 获取最新建筑状态
            BuildingModel building = buildingService.getBuilding(player.getPlayerId(), req.getBuildingType());
            // 3. 构造回包 (这里简化处理，实际根据 result 判断 code)
            RespBuildUpgrade.Builder resp = RespBuildUpgrade.newBuilder()
                    .setCode(result)
                    .setBuildingType(req.getBuildingType())
                    .setMsg(msg)
                    .setNewLevel(building.getLevel()) // 实际应从 Service 获取
                    .setUpgradeCompleteTime(building.getFinishTime());

            // 4. 发送回包 (封装进外层信封)
            GamePacket packet = GamePacket.newBuilder()
                    .setCmd(CmdId.RESP_BUILDING_UPGRADE)
                    .setContent(resp.build().toByteString())
                    .build();
            
            ctx.writeAndFlush(packet);

        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

    @GameHandler(cmd = CmdId.REQ_BUILDING_CREATE)
    public void create(ChannelHandlerContext ctx, PlayerModel player, byte[] data){
        try {
            ReqBuildCreate req = ReqBuildCreate.parseFrom(data);
            System.out.println("收到创建建筑请求，建筑类型: " + req.getBuildingType());
            boolean success = buildingService.createBuilding(player.getPlayerId(), req.getBuildingType());;

            String msg = success ? "创建成功" : "创建失败";
            RespBuildCreate.Builder resp = RespBuildCreate.newBuilder()
                    .setCode(success ? 0 : 1)
                    .setBuildingType(req.getBuildingType())
                    .setMsg(msg);
            // 4. 发送回包 (封装进外层信封)
            GamePacket packet = GamePacket.newBuilder()
                    .setCmd(CmdId.RESP_BUILDING_CREATE)
                    .setContent(resp.build().toByteString())
                    .build();

            ctx.writeAndFlush(packet);

        }catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }
}