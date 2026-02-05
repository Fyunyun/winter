package com.winter.modules.player;

import com.google.protobuf.GeneratedMessageV3;
import com.winter.common.model.PlayerModel;
import com.winter.msg.MsgId.CmdId;


import io.netty.channel.Channel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.winter.msg.PacketMsg.GamePacket;

public class PlayerManager {
    // 1. 维护 ID -> Channel 的映射
    private static final Map<Long, Channel> onlinePlayers = new ConcurrentHashMap<>();

    // 登录成功时调用
    public static void addPlayer(PlayerModel player, Channel channel) {
        onlinePlayers.put(player.getPlayerId(), channel);
        System.out.println("[玩家管理] 玩家上线，当前在线人数: " + onlinePlayers.size());
    }

    // 掉线时调用
    public static void removePlayer(long playerId) {
        onlinePlayers.remove(playerId);
        System.out.println("[玩家管理] 玩家下线，当前在线人数: " + onlinePlayers.size());
    }

    // 【核心】给指定玩家发包
    public static void sendToPlayer(long playerId, GeneratedMessageV3 message, CmdId cmd) {
        Channel channel = onlinePlayers.get(playerId);
        if (channel != null && channel.isActive()) {
            // 发送一个 send 方法用来打包
            GamePacket packet = GamePacket.newBuilder()
                    .setCmd(cmd)
                    .setContent(message.toByteString())
                    .build();
            channel.writeAndFlush(packet);
            System.out.println("推送消息给 " + playerId + ": " + cmd);
        }
    }

    // 查询玩家是否在线
    public static boolean isOnline(long playerId) {
        return onlinePlayers.containsKey(playerId);
    }
}
