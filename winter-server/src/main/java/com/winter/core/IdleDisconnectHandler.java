package com.winter.core;

import com.winter.core.util.SessionUtil;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class IdleDisconnectHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        Long playerId = SessionUtil.getPlayerId(ctx.channel());
        if (evt instanceof IdleStateEvent) {
        IdleStateEvent event = (IdleStateEvent) evt;
        
        // 如果是“读空闲”（Reader Idle），说明好久没收到玩家的数据了
        if (event.state() == IdleState.READER_IDLE) {
            
            // 处理心跳消息
            ctx.close().addListener((ChannelFutureListener) future -> {
                System.out.println("[系统] 玩家ID: " + playerId + " 已因长时间未响应被断开连接。");
            });
        }
        super.userEventTriggered(ctx, evt);
    }
    }
}