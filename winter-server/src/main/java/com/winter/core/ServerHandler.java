package com.winter.core; // 定义包名

import io.netty.channel.ChannelHandlerContext; // 导入Netty的ChannelHandlerContext类，用于处理通道上下文
import io.netty.channel.SimpleChannelInboundHandler; // 导入Netty的SimpleChannelInboundHandler类，用于处理入站消息

import com.winter.model.PlayerModel;
import com.winter.protocol.GamePacket; // 导入自定义的GamePacket类（未使用）
import com.winter.util.SessionUtil;
import com.winter.db.DataService;

// 定义一个继承SimpleChannelInboundHandler的类，用于处理String类型的入站消息
public class ServerHandler extends SimpleChannelInboundHandler<GamePacket> {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ServerHandler.class);

    // 当通道变为活跃状态时触发
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // 使用SLF4J记录新连接的客户端地址
        logger.info("[系统] 发现新幸存者连接: {}", ctx.channel().remoteAddress());
        // 向客户端发送欢迎消息
        // ctx.writeAndFlush("欢迎来到无尽冬日！请输入你的指令：\n");
    }

    // 处理从客户端接收到的消息
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GamePacket msg) {

        Long playerId = SessionUtil.getPlayerId(ctx.channel());

        PlayerModel player = WorldManager.onlinePlayers.get(playerId);

        // 模拟简单的游戏逻辑，根据指令内容返回不同的反馈
        if (msg.getType() == GamePacket.Type.FIRE) { // 如果指令包含"fire"

            if (player.getWood() >= 5) {
                player.setWood(player.getWood() - 5);
                DataService.updateResourceInRedis(player);

                GamePacket response = GamePacket.newBuilder()
                        .setType(GamePacket.Type.FIRE)
                        .setPlayerid(playerId)
                        .setContent("成功添加了5单位木材到火炉，当前木材剩余: " + player.getWood())
                        .build();

                ctx.writeAndFlush(response);
            } else {
                GamePacket response = GamePacket.newBuilder()
                        .setType(GamePacket.Type.FIRE)
                        .setPlayerid(playerId)
                        .setContent("当前木材剩余: " + player.getWood() + "，木材不足，无法添加到火炉。")
                        .build();

                ctx.writeAndFlush(response);
            }
        } else if (msg.getType() == GamePacket.Type.MOVE) { // 如果指令不匹配

            if (msg.getX() < 0 || msg.getY() < 0) {
                GamePacket response = GamePacket.newBuilder()
                        .setType(GamePacket.Type.MOVE)
                        .setPlayerid(playerId)
                        .setContent("移动失败，坐标不能为负数。")
                        .build();

                ctx.writeAndFlush(response);
                return;
            }
            player.setX(msg.getX());
            player.setY(msg.getY());
            DataService.updateResourceInRedis(player);

            GamePacket response = GamePacket.newBuilder()
                    .setType(GamePacket.Type.MOVE)
                    .setPlayerid(playerId)
                    .setX(msg.getX())
                    .setY(msg.getY())
                    .setContent("移动成功")
                    .build();

            ctx.writeAndFlush(response);
        } else if (msg.getType() == GamePacket.Type.FOOD) { // 采集食物
            player.setFood(player.getFood() + 10);
            DataService.updateResourceInRedis(player);

            GamePacket response = GamePacket.newBuilder()
                    .setType(GamePacket.Type.FOOD)
                    .setPlayerid(playerId)
                    .setContent("成功采集了10单位食物，当前食物总量: " + player.getFood())
                    .build();

            ctx.writeAndFlush(response);
        } else if (msg.getType() == GamePacket.Type.COAL) { // 采集煤炭
            player.setCoal(player.getCoal() + 8);
            DataService.updateResourceInRedis(player);

            GamePacket response = GamePacket.newBuilder()
                    .setType(GamePacket.Type.COAL)
                    .setPlayerid(playerId)
                    .setContent("成功采集了8单位煤炭，当前煤炭总量: " + player.getCoal())
                    .build();

            ctx.writeAndFlush(response);

        } else {
            // 提示客户端输入有效指令
            GamePacket response = GamePacket.newBuilder()
                    .setType(GamePacket.Type.UNKNOWN)
                    .setPlayerid(playerId)
                    .setContent("无效指令，请重新输入。")
                    .build();

            ctx.writeAndFlush(response);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Long playerId = SessionUtil.getPlayerId(ctx.channel());
        if (playerId != null && playerId > 0) {
            PlayerModel player = WorldManager.onlinePlayers.remove(playerId);
            if (player != null) {
                // 断线落库（或只标记脏数据，交给定时任务统一落库）
                DataService.flushToMysql(player);
            }
        }
        // 继续传播事件给 pipeline 后面的 handler（如果有）
        super.channelInactive(ctx);
    }

    // 当处理过程中发生异常时触发
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // 打印异常堆栈信息
        cause.printStackTrace();
        // 关闭当前通道
        ctx.close();
    }
}