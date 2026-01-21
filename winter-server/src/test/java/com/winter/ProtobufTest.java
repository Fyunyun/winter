// package com.winter;

// import com.winter.protocol.GamePacket; // 换成你生成的类名
// import com.google.protobuf.InvalidProtocolBufferException;
// import org.junit.Test;

// // import java.util.Arrays;

// public class ProtobufTest {

//     @Test
//     public void testSerialization() throws InvalidProtocolBufferException {
//         // 1. 【构建对象】使用 Builder 模式，不能直接 new
//         GamePacket originalMsg = GamePacket.newBuilder()
//                 .setType(GamePacket.Type.MOVE) // 枚举类型
//                 .setPlayerid(1001L)
//                 .setX(10.5f)
//                 .setY(20.8f)
//                 .setContent("玩家正在移动")
//                 .build();

//         System.out.println("--- 原始对象 ---");
//         System.out.println(originalMsg);

//         // 2. 【序列化】将 Java 对象转为字节数组 (这就是要在网络上传输的数据)
//         byte[] rawData = originalMsg.toByteArray();
        
//         System.out.println("--- 序列化后的二进制数据 ---");
//         System.out.println("数据长度: " + rawData.length + " 字节");
//         System.out.println("十六进制内容: " + bytesToHex(rawData));

//         // 3. 【反序列化】将字节数组还原回 Java 对象
//         GamePacket decodedMsg = GamePacket.parseFrom(rawData);

//         System.out.println("--- 反序列化后的对象 ---");
//         System.out.println("玩家ID: " + decodedMsg.getPlayerid());
//         System.out.println("消息内容: " + decodedMsg.getContent());
        
//         // 验证一致性
//         assert decodedMsg.getPlayerid() == originalMsg.getPlayerid();
//     }

//     // 辅助工具：将字节转为十六进制字符串显示
//     private String bytesToHex(byte[] bytes) {
//         StringBuilder sb = new StringBuilder();
//         for (byte b : bytes) {
//             sb.append(String.format("%02X ", b));
//         }
//         return sb.toString();
//     }
// }