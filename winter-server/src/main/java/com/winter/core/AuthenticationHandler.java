// package com.winter.core; // 定义包名

// import com.winter.common.model.PlayerModel;
// import com.winter.msg.PacketMsg.GamePacket;
// import com.winter.modules.auth.LoginService;
// import com.winter.core.util.SessionUtil;

// import io.netty.channel.ChannelFutureListener;
// import io.netty.channel.ChannelHandlerContext; 
// import io.netty.channel.SimpleChannelInboundHandler;

// // 定义一个继承自ChannelInboundHandlerAdapter的类，用于处理入站消息
// public class AuthenticationHandler extends SimpleChannelInboundHandler<GamePacket> {
//     // 重写channelRead方法，用于处理接收到的消息
//     @Override
//     public void channelRead0(ChannelHandlerContext ctx, GamePacket msg) throws Exception {
//         if (msg.getType() != GamePacket.Type.LOGIN) {
//             if (!SessionUtil.hasLogin(ctx.channel())) {
//                 GamePacket resp = GamePacket.newBuilder()
//                         .setType(GamePacket.Type.LOGIN)
//                         .setPlayerid(0)
//                         .setContent("请先登录")
//                         .build();
//                 ctx.writeAndFlush(resp);
//                 return;
//             }
//             ctx.fireChannelRead(msg);
//             return;
//         }

//         PlayerModel player = LoginService.handleLogin(msg.getUsername(), msg.getPassword());
//         if (player != null) {
//             GamePacket resp = GamePacket.newBuilder()
//                     .setType(GamePacket.Type.LOGIN)
//                     .setPlayerid(player.getPlayerId())
//                     .setContent("登录成功")
//                     .build();
//             ctx.writeAndFlush(resp);
//             SessionUtil.bindPlayerId(ctx.channel(), player.getPlayerId());
//             WorldManager.onlinePlayers.put(player.getPlayerId(), player);

//             // 可选：登录成功后移除认证 handler，避免每条消息都判断一次
//             ctx.pipeline().remove(this);
//         } else {
//             GamePacket resp = GamePacket.newBuilder()
//                     .setType(GamePacket.Type.LOGIN)
//                     .setPlayerid(0)
//                     .setContent("登录失败，账号或密码错误")
//                     .build();
//             ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
//             return;
//         }
//     }
// }