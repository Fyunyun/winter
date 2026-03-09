package com.winter.modules.chat;

import org.springframework.stereotype.Service;
import com.winter.common.model.PlayerModel;
import com.winter.core.db.DbManager;
import com.winter.modules.chat.util.SensitiveWordFilter;
import com.winter.modules.player.PlayerManager;
import com.winter.msg.ChatMsg;
import org.springframework.beans.factory.annotation.Autowired;
import com.winter.msg.ChatMsg.BrdPrivateChat;
import com.winter.msg.MsgId.CmdId;

import redis.clients.jedis.Jedis;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatService {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ChatService.class);

    @Autowired
    private SensitiveWordFilter wordFilterService;

    /**
     * 聊天发言频率限制：每个玩家的上次发言时间戳（毫秒）
     * 私聊限制：同一玩家每秒最多发 5 条
     * 世界广播限制：同一玩家每 2 秒最多发 1 条
     */
    private static final Map<Long, Long> privateChatCooldown = new ConcurrentHashMap<>();
    private static final Map<Long, Long> broadcastCooldown = new ConcurrentHashMap<>();

    private static final long PRIVATE_CHAT_INTERVAL_MS = 200;    // 私聊最小间隔 200ms（每秒最多 5 条）
    private static final long BROADCAST_INTERVAL_MS = 2000;      // 世界广播最小间隔 2 秒
    private static final int MAX_OFFLINE_MESSAGES = 100;         // 每人最多存 100 条离线消息

    public void sendPrivateChat(PlayerModel sender, long targetId, String message, int Msgtype) {
        if (sender.getPlayerId() == targetId) {
            logger.warn("玩家 {} 尝试给自己发消息", sender.getPlayerId());
            return;
        }
        if (message == null || message.trim().isEmpty()) {
            logger.warn("玩家 {} 发送空消息", sender.getPlayerId());
            return;
        }
        if (message.length() > 200) {
            logger.warn("玩家 {} 消息太长: {} 字符", sender.getPlayerId(), message.length());
            return;
        }

        // 频率限制检查
        if (!checkRateLimit(privateChatCooldown, sender.getPlayerId(), PRIVATE_CHAT_INTERVAL_MS)) {
            logger.warn("玩家 {} 私聊发言过于频繁", sender.getPlayerId());
            return;
        }

        String CleanMessage = wordFilterService.filter(message);

        BrdPrivateChat chatMsg = BrdPrivateChat.newBuilder()
                .setFromId(sender.getPlayerId())
                .setFromName(sender.getName())
                .setContent(CleanMessage)
                .setMsgType(Msgtype)
                .setTimestamp(System.currentTimeMillis())
                .build();

        boolean isTargetOnline = PlayerManager.isOnline(targetId);

        if (isTargetOnline) {
            PlayerManager.sendToPlayer(targetId, chatMsg, CmdId.BRD_PRIVATE_CHAT);
        } else {
            saveOfflineMessage(targetId, chatMsg);
        }
    }

    private void saveOfflineMessage(long targetId, ChatMsg.BrdPrivateChat message) {
        try (Jedis redis = DbManager.getJedis()) {
            byte[] key = ("chat:offline:" + targetId).getBytes();
            // 将 Proto 对象序列化为字节数组存入 Redis
            redis.rpush(key, message.toByteArray());

            // 裁剪列表，只保留最近 MAX_OFFLINE_MESSAGES 条，防止离线消息无限增长
            redis.ltrim(key, -MAX_OFFLINE_MESSAGES, -1);

            // 设置过期时间，离线消息只保留 7 天
            redis.expire(key, 60 * 60 * 24 * 7);
        }
    }

    public void broadcastToAll(PlayerModel sender, String message) {
        // 世界广播频率限制
        if (!checkRateLimit(broadcastCooldown, sender.getPlayerId(), BROADCAST_INTERVAL_MS)) {
            logger.warn("玩家 {} 世界广播发言过于频繁", sender.getPlayerId());
            return;
        }

        String cleanMessage = wordFilterService.filter(message);
        ChatMsg.BrdGroupChat chatMsg = ChatMsg.BrdGroupChat.newBuilder()
                .setFromId(sender.getPlayerId())
                .setFromName(sender.getName())
                .setContent(cleanMessage)
                .setTimestamp(System.currentTimeMillis())
                .build();
        PlayerManager.broadcast(chatMsg, CmdId.BRD_BROADCAST_CHAT);
    }

    /**
     * 频率限制检查（令牌桶简化版）
     *
     * @param cooldownMap 存储各玩家上次执行时间的 Map
     * @param playerId   玩家 ID
     * @param intervalMs 最小间隔（毫秒）
     * @return true=通过检查, false=被限流
     */
    private boolean checkRateLimit(Map<Long, Long> cooldownMap, long playerId, long intervalMs) {
        long now = System.currentTimeMillis();
        Long lastTime = cooldownMap.get(playerId);
        if (lastTime != null && (now - lastTime) < intervalMs) {
            return false;   // 间隔太短，拒绝
        }
        cooldownMap.put(playerId, now);
        return true;
    }
}
