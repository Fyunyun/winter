package com.winter.core.net.handler; // 定义包名

import io.netty.channel.ChannelHandlerContext; // 导入Netty的ChannelHandlerContext类，用于处理通道上下文
import io.netty.channel.SimpleChannelInboundHandler; // 导入Netty的SimpleChannelInboundHandler类，用于处理入站消息
import com.winter.core.db.DataService;
import com.winter.core.util.SessionUtil;

import com.winter.common.model.PlayerModel;

import com.winter.msg.PacketMsg.GamePacket;

import com.winter.core.router.MessageDispatcher;


// 定义一个继承SimpleChannelInboundHandler的类，用于处理String类型的入站消息
public class ServerHandler extends SimpleChannelInboundHandler<GamePacket> {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ServerHandler.class);

    // 当通道变为活跃状态时触发
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        logger.info("[系统] 发现新幸存者连接: {}", ctx.channel().remoteAddress());
    }

    // 处理从客户端接收到的消息
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GamePacket msg) {
        System.out.println("[接收] cmd=" + msg.getCmd() + ", size=" + msg.getContent().size());
        MessageDispatcher.dispatch(ctx, msg);

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Long playerId = SessionUtil.getPlayerId(ctx.channel());
        if (playerId != null && playerId > 0) {
            // 玩家断线处理 从redis获取玩家
            PlayerModel player = DataService.loadPlayerFromRedis(playerId);
            if (player != null) {
                // 断线落库（或只标记脏数据，交给定时任务统一落库）
                DataService.flushToMysql(player);
                DataService.clearPlayerDataInRedis(playerId);
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