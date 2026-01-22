package com.winter;

// 引入 Protobuf 生成的消息类型（你们的协议包）
// GamePacket 通常是 .proto 编译后生成的 Java 类
import com.winter.protocol.GamePacket;

// Netty 客户端启动器
import io.netty.bootstrap.Bootstrap;

// Netty 通道相关：Channel、Handler、Context 等
import io.netty.channel.*;

// NIO 事件循环线程组（负责网络事件的处理线程）
import io.netty.channel.nio.NioEventLoopGroup;

// SocketChannel：基于 TCP 的通道类型
import io.netty.channel.socket.SocketChannel;

// NIO 版本的客户端 SocketChannel 实现
import io.netty.channel.socket.nio.NioSocketChannel;

// Netty 提供的 Protobuf 编解码器（含长度字段处理）
import io.netty.handler.codec.protobuf.*;

// 控制台输入读取
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * 测试用的游戏客户端（使用 Netty + Protobuf）。
 *
 * 功能：
 * 1) 连接到本地 127.0.0.1:8088
 * 2) 从控制台读取命令，封装为 GamePacket 发送给服务器
 * 3) 接收服务器返回的 GamePacket 并打印
 *
 * 注意：该类是 test 目录下的测试客户端，不是正式客户端工程结构。
 */
public class GameClientTest {

    /**
     * 当前客户端的玩家 ID（默认值）。
     * 在输入 login [ID] 后会被更新为新的 ID。
     *
     * 用 static 的原因：main 线程和 handler 回调共享该值更方便（这里不做并发保护）。
     */
    private static long myPlayerId = 0;

    /**
     * 程序入口：
     * - 启动 Netty 客户端
     * - 配置 pipeline（Protobuf 编解码）
     * - 读取控制台指令发送消息
     */
    public static void main(String[] args) throws Exception {

        // 事件循环组：本质是一组线程（Reactor 线程池），负责 IO 事件的派发与处理
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            // Bootstrap：Netty 客户端引导类，用于创建并连接客户端 Channel
            Bootstrap b = new Bootstrap();

            // 配置客户端：
            // - group：设置 EventLoopGroup
            // - channel：设置客户端通道实现（NIO TCP）
            // - handler：设置 Channel 初始化器，用于给 pipeline 安装各种 Handler
            b.group(group)
             .channel(NioSocketChannel.class)
             .handler(new ChannelInitializer<SocketChannel>() {

                 /**
                  * 当 Channel 创建完成后会回调 initChannel，用于初始化 pipeline。
                  */
                 @Override
                 protected void initChannel(SocketChannel ch) {

                     // pipeline：Netty 的处理链（入站/出站事件会按顺序经过这些 handler）

                     // 入站（Inbound）解码部分：
                     // 1) ProtobufVarint32FrameDecoder
                     //    - 解决“粘包/半包”问题
                     //    - 按 protobuf varint32 长度字段切分完整消息帧
                     ch.pipeline().addLast(new ProtobufVarint32FrameDecoder());

                     // 2) ProtobufDecoder
                     //    - 将 ByteBuf 解码为具体的 Protobuf 消息对象（这里是 GamePacket）
                     //    - getDefaultInstance() 用于告诉 decoder 目标消息类型
                     ch.pipeline().addLast(new ProtobufDecoder(GamePacket.getDefaultInstance()));

                     // 出站（Outbound）编码部分：
                     // 3) ProtobufVarint32LengthFieldPrepender
                     //    - 在 protobuf 消息前写入 varint32 长度字段
                     //    - 配合 ProtobufVarint32FrameDecoder 使用
                     ch.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());

                     // 4) ProtobufEncoder
                     //    - 将 Protobuf 对象编码成 ByteBuf
                     ch.pipeline().addLast(new ProtobufEncoder());

                     // 业务处理 Handler（入站）：
                     // SimpleChannelInboundHandler<GamePacket> 会自动释放 msg（引用计数对象）
                     ch.pipeline().addLast(new SimpleChannelInboundHandler<GamePacket>() {

                         /**
                          * 收到服务器消息时触发（已经被 ProtobufDecoder 解码成 GamePacket）。
                          */
                         @Override
                         protected void channelRead0(ChannelHandlerContext ctx, GamePacket msg) {
                             // 打印服务器返回的消息内容
                             System.out.println("\n>>> [服务器响应] " + msg.getContent());

                             // 如果服务器返回的是 LOGIN，更新本地 playerId
                             if (msg.getType() == GamePacket.Type.LOGIN && msg.getPlayerid() > 0) {
                                 myPlayerId = msg.getPlayerid();
                                 System.out.println("    [客户端] 已更新 playerId = " + myPlayerId);
                             }

                             // 如果服务器返回的是 FIRE 类型，额外打印一点提示
                             if (msg.getType() == GamePacket.Type.FIRE) {
                                 System.out.println("    ( ^_^)o自自自  <-- 暖和了！");
                             }

                             // 重新打印输入提示符（因为服务端消息会打断你的输入行）
                             System.out.print("请输入指令 > ");
                         }

                         /**
                          * 连接断开时触发（例如服务端关闭、网络异常等）。
                          */
                         @Override
                         public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                             System.out.println("\n[系统] 已与服务器断开连接。");
                             super.channelInactive(ctx);
                         }
                     });
                 }
             });

            // 发起 TCP 连接到服务器：
            // sync() 等待连接完成；channel() 获取已连接的 Channel
            Channel channel = b.connect("127.0.0.1", 8088).sync().channel();

            // 打印使用说明（控制台命令）
            System.out.println("============== 凛冬客户端 (Protobuf版) ==============");
            System.out.println("1. 登录: login [username] [password]  (例如: login winter_001 pwd123)");
            System.out.println("2. 移动: move [x] [y]                 (例如: move 10.5 20)");
            System.out.println("3. 生火: fire                         (直接输入 fire)");
            System.out.println("4. 采集食物: food                     (直接输入 food)");
            System.out.println("5. 采集煤炭: coal                     (直接输入 coal)");
            System.out.println("6. 心跳: heartbeat                    (直接输入 heartbeat)");
            System.out.println("7. 采集木头: wood                     (直接输入 wood)");
            System.out.println("8. 建筑升级: building_upgrade [buildingType]  (例如: building_upgrade 1)");
            System.out.println("9. 完成建筑升级: building_complete [buildingType]  (例如: building_complete 1)");
            System.out.println("10. 退出: quit");
            System.out.println("===================================================");
            System.out.print("请输入指令 > ");

            // 控制台读取：从标准输入读取一行一行的命令
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String line;

            // 主循环：不停读取用户输入直到 EOF 或 quit
            while ((line = reader.readLine()) != null) {

                // 用户输入 quit 则退出循环（随后会进入 finally 关闭线程组）
                if (line.trim().equalsIgnoreCase("quit")) {
                    break;
                }

                // 按空白拆分参数（支持多个空格、tab）
                String[] parts = line.trim().split("\\s+");

                // 防御：空输入直接跳过
                if (parts.length == 0) continue;

                // 命令关键字（统一转小写处理）
                String command = parts[0].toLowerCase();

                // 创建一个 GamePacket 的 builder，用于组装要发送的消息
                GamePacket.Builder builder = GamePacket.newBuilder();

                // 默认带上当前 playerId（你的协议字段名是 playerid，保持原样）
                builder.setPlayerid(myPlayerId);

                try {
                    // 命令：login [username] [password]
                    if (command.equals("login")) {
                        // 参数不足时提示正确格式
                        if (parts.length < 3) {
                            System.out.println("格式错误，请使用: login [username] [password]");
                            continue;
                        }

                        String username = parts[1];
                        String password = parts[2];

                        // 设置 builder 字段：type / username / password
                        builder.setType(GamePacket.Type.LOGIN);
                        builder.setUsername(username);
                        builder.setPassword(password);
                        builder.setContent("请求登录");

                        // 本地打印发送日志
                        System.out.println("[发送] 登录请求 Username:" + username);

                    }
                    // 命令：move [x] [y]
                    else if (command.equals("move")) {
                        // 参数不足时提示正确格式
                        if (parts.length < 3) {
                            System.out.println("格式错误，请使用: move [x] [y]");
                            continue;
                        }

                        // 解析坐标
                        float x = Float.parseFloat(parts[1]);
                        float y = Float.parseFloat(parts[2]);

                        // 设置移动消息（type + 坐标 + content）
                        builder.setType(GamePacket.Type.MOVE);
                        builder.setX(x);
                        builder.setY(y);
                        builder.setContent("玩家正在移动");

                        // 本地打印发送日志
                        System.out.println("[发送] 移动请求 -> (" + x + ", " + y + ")");

                    }
                    // 命令：fire（无参数）
                    else if (command.equals("fire")) {
                        // 设置生火消息
                        builder.setType(GamePacket.Type.FIRE);

                        // content 只是附带说明，关键是 type
                        builder.setContent("请求生火");

                        // 本地打印发送日志
                        System.out.println("[发送] 正在往熔炉里添柴火...");
                    }
                    // 命令：food（无参数）
                    else if (command.equals("food")) {
                        builder.setType(GamePacket.Type.FOOD);
                        builder.setContent("请求采集食物");
                        System.out.println("[发送] 正在采集食物...");
                    }
                    // 命令：coal（无参数）
                    else if (command.equals("coal")) {
                        builder.setType(GamePacket.Type.COAL);
                        builder.setContent("请求采集煤炭");
                        System.out.println("[发送] 正在采集煤炭...");
                    }
                    // 命令：heartbeat（无参数）
                    else if (command.equals("heartbeat")) {
                        builder.setType(GamePacket.Type.HEARTBEAT);
                        builder.setContent("心跳");
                        System.out.println("[发送] 心跳");
                    }else if (command.equals("building_upgrade")) {
                        // 参数不足时提示正确格式
                        if (parts.length < 2) {
                            System.out.println("格式错误，请使用: building_upgrade [buildingType]");
                            continue;
                        }

                        int buildingType = Integer.parseInt(parts[1]);

                        // 设置建筑升级消息
                        builder.setType(GamePacket.Type.BUILDING_UPGRADE);
                        builder.setBuildingType(buildingType);
                        builder.setContent("请求建筑升级");

                        // 本地打印发送日志
                        System.out.println("[发送] 建筑升级请求 BuildingType:" + buildingType);

                    } else if (command.equals("building_complete")) {
                        // 参数不足时提示正确格式
                        if (parts.length < 2) {
                            System.out.println("格式错误，请使用: building_complete [buildingType]");
                            continue;
                        }

                        int buildingType = Integer.parseInt(parts[1]);

                        // 设置完成建筑升级消息
                        builder.setType(GamePacket.Type.BUILDING_COMPLETE);
                        builder.setBuildingType(buildingType);
                        builder.setContent("请求完成建筑升级");

                        // 本地打印发送日志
                        System.out.println("[发送] 完成建筑升级请求 BuildingType:" + buildingType);

                    }else if (command.equals("wood")) {
                        builder.setType(GamePacket.Type.WOOD);
                        builder.setContent("请求采集木材");
                        System.out.println("[发送] 正在采集木材...");
                    }
                    // 其它未知命令：当做普通消息发给服务器
                    else {
                        builder.setType(GamePacket.Type.UNKNOWN);
                        builder.setContent(line);
                        System.out.println("[发送] 普通消息: " + line);
                    }

                    // 将构建好的 GamePacket 写入并刷出到网络
                    // writeAndFlush：写入 ChannelOutboundBuffer 并立即触发 flush
                    channel.writeAndFlush(builder.build());

                } catch (NumberFormatException e) {
                    // 当 login/move 参数无法解析为数字时会进这里
                    System.out.println("输入格式错误：数字解析失败！");
                } catch (Exception e) {
                    // 兜底异常处理（测试代码可用；生产建议做更细粒度处理）
                    e.printStackTrace();
                }
            }

        } finally {
            // 优雅关闭 EventLoopGroup（释放线程与资源）
            // 即使发生异常也会执行，避免线程泄露
            group.shutdownGracefully();
        }
    }
}