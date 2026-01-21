package com.winter.util;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

public class SessionUtil {

    public static final AttributeKey<Long> PLAYER_ID =
            AttributeKey.valueOf("playerid");

    // 检查是否登录：必须已经绑定了 playerId（值非 null）
    public static boolean hasLogin(Channel channel) {
        return channel.attr(PLAYER_ID).get() != null;
    }

    public static Long getPlayerId(Channel channel) {
        return channel.attr(PLAYER_ID).get();
    }

    // 建议补一个绑定方法，避免到处直接 attr().set()
    public static void bindPlayerId(Channel channel, long playerId) {
        channel.attr(PLAYER_ID).set(playerId);
        // System.out.println("!!!!!!!!!!!!!" + channel);
    }

    // 可选：下线/断线时清理
    public static void unbind(Channel channel) {
        channel.attr(PLAYER_ID).set(null);
    }
}