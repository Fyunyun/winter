package com.winter.core;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
// import io.netty.handler.codec.string.StringDecoder;
// import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.codec.protobuf.*;

import com.winter.db.DataService;
import com.winter.model.PlayerModel;
import com.winter.protocol.GamePacket;

// 使用 Telnet 测试服务器：telnet localhost 8088

/**
 * 游戏服务器类，基于 Netty 实现
 */
public class GameServer {
    private final int port; // 服务器监听的端口号

    /**
     * 构造函数，初始化服务器端口
     * 
     * @param port 监听的端口号
     */
    public GameServer(int port) {
        this.port = port;
    }

    /**
     * 启动服务器
     * 
     * @throws InterruptedException 如果线程被中断
     */
    public void start() throws InterruptedException {
        // 1. 创建负责接收客户端连接的线程组（bossGroup）
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        // 2. 创建负责处理客户端请求的线程组（workerGroup）
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            // 3. 创建服务器启动器
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup) // 设置线程组
                    .channel(NioServerSocketChannel.class) // 指定服务端通道类型
                    .childHandler(new ChannelInitializer<SocketChannel>() { // 初始化每个客户端连接的通道
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            // // 4. 配置流水线，添加处理器
                            // // 添加字符串解码器，方便接收字符串数据
                            // ch.pipeline().addLast(new StringDecoder());
                            // // 添加字符串编码器，方便发送字符串数据
                            // ch.pipeline().addLast(new StringEncoder());

                            // 1. 添加心跳检测 (放在最前面或编解码器之后)
                            // 参数分别代表：读空闲时间、写空闲时间、读写空闲时间、时间单位
                            // 这里设置为：如果 10 秒钟没有收到客户端发来的数据，就触发一次“读空闲”事件
                            ch.pipeline().addLast(new IdleStateHandler(120, 0, 0, TimeUnit.SECONDS));
                            ch.pipeline().addLast(new IdleDisconnectHandler());

                            // 2. [核心变化] Protobuf 处理链
                            // 2.1 处理半包/粘包 (告诉 Netty 数据包的长度)
                            ch.pipeline().addLast(new ProtobufVarint32FrameDecoder());

                            // 2.2 具体的解码器 (告诉 Netty 把二进制转成 GamePacket 对象)
                            ch.pipeline().addLast(new ProtobufDecoder(GamePacket.getDefaultInstance()));

                            // 2.3 编码器 (用于服务器给客户端发消息)
                            ch.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
                            ch.pipeline().addLast(new ProtobufEncoder());

                            // 添加认证处理器
                            ch.pipeline().addLast(new AuthenticationHandler());
                            // 添加游戏逻辑的业务处理器
                            ch.pipeline().addLast(new ServerHandler());
                        }
                    });

            // 5. 启动服务器并绑定端口
            System.out.println(">>> ❄️ 冬日游戏服务器已在端口 " + port + " 启动...");
            ChannelFuture f = b.bind(port).sync(); // 同步等待绑定完成
            f.channel().closeFuture().sync(); // 同步等待服务器关闭
        } finally {
            // 6. 优雅关闭线程组，释放资源
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    /**
     * 主方法，启动服务器
     * 
     * @param args 命令行参数
     * @throws InterruptedException 如果线程被中断
     */
    public static void main(String[] args) throws InterruptedException {

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            System.out.println(">>> 执行全服数据存盘...");
            for (PlayerModel player : WorldManager.onlinePlayers.values()) {
                // 将 Redis/内存中的数据持久化到 MySQL
                DataService.flushToMysql(player);
            }
        }, 5, 5, TimeUnit.MINUTES); // 每5分钟存一次

        // 创建服务器实例并启动，监听端口 8088
        new GameServer(8088).start();
    }
}