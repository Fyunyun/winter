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

@Service
public class ChatService {

    @Autowired
    private SensitiveWordFilter wordFilterService;

    public void sendPrivateChat(PlayerModel sender, long targetId, String message, int Msgtype) {
        if (sender.getPlayerId() == targetId) {
            System.out.println("不能给自己发消息");
            return;
        }
        if (message == null || message.trim().isEmpty()) {
            System.out.println("不能发送空消息");
            return;
        }
        if (message.length() > 200) {
            System.out.println("消息太长，最大长度为200个字符");
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
            PlayerManager.sendToPlayer(targetId, chatMsg, com.winter.msg.MsgId.CmdId.BRD_PRIVATE_CHAT);
        } else {
            saveOfflineMessage(targetId, chatMsg);
        }
    }

    private void saveOfflineMessage(long targetId, ChatMsg.BrdPrivateChat message) {
        try (Jedis redis = DbManager.getJedis()) {
            String key = "chat:offline:" + targetId;
            // 将 Proto 对象序列化为字节数组存入 Redis
            // 注意：Jedis 操作 byte[] 需要使用对应的方法
            redis.rpush(key.getBytes(), message.toByteArray());

            // 设置过期时间，比如离线消息只保留 7 天
            redis.expire(key.getBytes(), 60 * 60 * 24 * 7);
        }
    }

    public void broadcastToAll(PlayerModel sender, String message) {
        String cleanMessage = wordFilterService.filter(message);
        ChatMsg.BrdGroupChat chatMsg = ChatMsg.BrdGroupChat.newBuilder()
                .setFromId(sender.getPlayerId())
                .setFromName(sender.getName())
                .setContent(cleanMessage)
                .setTimestamp(System.currentTimeMillis())
                .build();
        PlayerManager.broadcast(chatMsg, CmdId.BRD_BROADCAST_CHAT);
    }
}
