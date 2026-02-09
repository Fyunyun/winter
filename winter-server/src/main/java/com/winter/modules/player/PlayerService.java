package com.winter.modules.player;

import java.util.List;

import org.springframework.stereotype.Service;

import com.google.protobuf.InvalidProtocolBufferException;
import com.winter.common.model.PlayerModel;
import com.winter.core.db.DbManager;
import com.winter.msg.ChatMsg;
import com.winter.msg.MsgId.CmdId;

import redis.clients.jedis.Jedis;

@Service
public class PlayerService {

    // 在玩家登录成功，且加载完基础数据后调用
    public void checkOfflineMessages(PlayerModel player) {
        try (Jedis redis = DbManager.getJedis()) {
            String key = "chat:offline:" + player.getPlayerId();
            byte[] keyBytes = key.getBytes();

            // 1. 检查有没有消息
            if (redis.exists(keyBytes)) {
                // 2. 一次性取出所有消息 (LRAange 0, -1)
                List<byte[]> offlineMsgs = redis.lrange(keyBytes, 0, -1);

                if (offlineMsgs != null && !offlineMsgs.isEmpty()) {
                    for (byte[] msgBytes : offlineMsgs) {
                        try {
                            // 反序列化
                            ChatMsg.BrdPrivateChat msg = ChatMsg.BrdPrivateChat.parseFrom(msgBytes);

                            // 发送给玩家
                            PlayerManager.sendToPlayer(player.getPlayerId(), msg, CmdId.BRD_PRIVATE_CHAT);
                        } catch (InvalidProtocolBufferException e) {
                            e.printStackTrace();
                        }
                    }
                }
                // 3. 推送完后，清空 Redis 队列
                redis.del(keyBytes);
            }
        }
    }
}
