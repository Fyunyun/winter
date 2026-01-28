package com.winter;

import com.google.protobuf.ByteString;
import com.winter.msg.AuthMsg.ReqLogin;
import com.winter.msg.AuthMsg.RespLogin;
import com.winter.msg.BuildingMsg.ReqBuildCreate;
import com.winter.msg.BuildingMsg.ReqBuildUpgrade;
import com.winter.msg.BuildingMsg.RespBuildCreate;
import com.winter.msg.BuildingMsg.RespBuildUpgrade;
import com.winter.msg.CollectMsg.ReqCollect;
import com.winter.msg.CollectMsg.RespCollect;
import com.winter.msg.MoveMsg.ReqMove;
import com.winter.msg.MoveMsg.RespMove;
import com.winter.msg.MsgId.CmdId;
import com.winter.msg.PacketMsg.GamePacket;
import com.winter.msg.RegisterMsg.ReqRegister;
import com.winter.msg.RegisterMsg.RespRegister;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Locale;

public class ConsoleTestClient {

    private static volatile long playerId = 0;

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 8088;

        EventLoopGroup group = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new ProtobufVarint32FrameDecoder());
                            ch.pipeline().addLast(new ProtobufDecoder(GamePacket.getDefaultInstance()));
                            ch.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
                            ch.pipeline().addLast(new ProtobufEncoder());
                            ch.pipeline().addLast(new ClientHandler());
                        }
                    });

            Channel channel = bootstrap.connect(host, port).sync().channel();

            printHelp();
            System.out.print("请输入指令 > ");

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    System.out.print("请输入指令 > ");
                    continue;
                }

                if (trimmed.equalsIgnoreCase("quit") || trimmed.equalsIgnoreCase("exit")) {
                    break;
                }

                if (trimmed.equalsIgnoreCase("help")) {
                    printHelp();
                    System.out.print("请输入指令 > ");
                    continue;
                }

                String[] parts = trimmed.split("\\s+");
                String command = parts[0].toLowerCase(Locale.ROOT);

                try {
                    switch (command) {
                        case "register":
                            handleRegister(channel, parts);
                            break;
                        case "login":
                            handleLogin(channel, parts);
                            break;
                        case "move":
                            handleMove(channel, parts);
                            break;
                        case "collect":
                            handleCollect(channel, parts);
                            break;
                        case "building_create":
                            handleBuildingCreate(channel, parts);
                            break;
                        case "building_upgrade":
                            handleBuildingUpgrade(channel, parts);
                            break;
                        case "all":
                            handleAllFlow(channel, parts);
                            break;
                        default:
                            System.out.println("未知指令: " + command + "，请输入 help 查看可用指令。");
                            break;
                    }
                } catch (Exception e) {
                    System.out.println("指令处理失败: " + e.getMessage());
                    e.printStackTrace();
                }

                System.out.print("请输入指令 > ");
            }

        } finally {
            group.shutdownGracefully();
        }
    }

    private static void handleRegister(Channel channel, String[] parts) {
        if (parts.length < 3) {
            System.out.println("格式错误，请使用: register <username> <password>");
            return;
        }
        ReqRegister req = ReqRegister.newBuilder()
                .setUsername(parts[1])
                .setPassword(parts[2])
                .build();
        sendPacket(channel, CmdId.REQ_REGISTER, req.toByteString());
        System.out.println("[发送] 注册请求: " + parts[1]);
    }

    private static void handleLogin(Channel channel, String[] parts) {
        if (parts.length < 3) {
            System.out.println("格式错误，请使用: login <username> <password>");
            return;
        }
        ReqLogin req = ReqLogin.newBuilder()
                .setUsername(parts[1])
                .setPassword(parts[2])
                .build();
        sendPacket(channel, CmdId.REQ_LOGIN, req.toByteString());
        System.out.println("[发送] 登录请求: " + parts[1]);
    }

    private static void handleMove(Channel channel, String[] parts) {
        if (parts.length < 3) {
            System.out.println("格式错误，请使用: move <x> <y>");
            return;
        }
        float x = Float.parseFloat(parts[1]);
        float y = Float.parseFloat(parts[2]);
        ReqMove req = ReqMove.newBuilder()
                .setX(x)
                .setY(y)
                .build();
        sendPacket(channel, CmdId.REQ_MOVE, req.toByteString());
        System.out.println("[发送] 移动请求: (" + x + ", " + y + ")");
    }

    private static void handleCollect(Channel channel, String[] parts) {
        if (parts.length < 2) {
            System.out.println("格式错误，请使用: collect <coal|wood|food> [amount]");
            return;
        }
        String type = parts[1].toLowerCase(Locale.ROOT);
        int amount = parts.length >= 3 ? Integer.parseInt(parts[2]) : 1;

        int resourceType;
        CmdId cmdId;
        switch (type) {
            case "coal":
                resourceType = 1;
                cmdId = CmdId.REQ_COLLECT_COAL;
                break;
            case "wood":
                resourceType = 2;
                cmdId = CmdId.REQ_COLLECT_WOOD;
                break;
            case "food":
                resourceType = 3;
                cmdId = CmdId.REQ_COLLECT_FOOD;
                break;
            default:
                System.out.println("未知资源类型: " + type + "，可选 coal/wood/food");
                return;
        }

        ReqCollect req = ReqCollect.newBuilder()
                .setResourceType(resourceType)
                .setAmount(amount)
                .build();
        sendPacket(channel, cmdId, req.toByteString());
        System.out.println("[发送] 采集请求: " + type + " x" + amount);
    }

    private static void handleBuildingCreate(Channel channel, String[] parts) {
        if (parts.length < 2) {
            System.out.println("格式错误，请使用: building_create <buildingType>");
            return;
        }
        int buildingType = Integer.parseInt(parts[1]);
        ReqBuildCreate req = ReqBuildCreate.newBuilder()
                .setBuildingType(buildingType)
                .build();
        sendPacket(channel, CmdId.REQ_BUILDING_CREATE, req.toByteString());
        System.out.println("[发送] 建筑创建请求: type=" + buildingType);
    }

    private static void handleBuildingUpgrade(Channel channel, String[] parts) {
        if (parts.length < 2) {
            System.out.println("格式错误，请使用: building_upgrade <buildingType>");
            return;
        }
        int buildingType = Integer.parseInt(parts[1]);
        ReqBuildUpgrade req = ReqBuildUpgrade.newBuilder()
                .setBuildingType(buildingType)
                .build();
        sendPacket(channel, CmdId.REQ_BUILDING_UPGRADE, req.toByteString());
        System.out.println("[发送] 建筑升级请求: type=" + buildingType);
    }

    private static void handleAllFlow(Channel channel, String[] parts) {
        if (parts.length < 3) {
            System.out.println("格式错误，请使用: all <username> <password>");
            return;
        }
        String username = parts[1];
        String password = parts[2];

        ReqRegister registerReq = ReqRegister.newBuilder()
                .setUsername(username)
                .setPassword(password)
                .build();
        sendPacket(channel, CmdId.REQ_REGISTER, registerReq.toByteString());

        ReqLogin loginReq = ReqLogin.newBuilder()
                .setUsername(username)
                .setPassword(password)
                .build();
        sendPacket(channel, CmdId.REQ_LOGIN, loginReq.toByteString());

        ReqCollect collectWood = ReqCollect.newBuilder()
                .setResourceType(2)
                .setAmount(1)
                .build();
        sendPacket(channel, CmdId.REQ_COLLECT_WOOD, collectWood.toByteString());

        ReqCollect collectCoal = ReqCollect.newBuilder()
                .setResourceType(1)
                .setAmount(1)
                .build();
        sendPacket(channel, CmdId.REQ_COLLECT_COAL, collectCoal.toByteString());

        ReqCollect collectFood = ReqCollect.newBuilder()
                .setResourceType(3)
                .setAmount(1)
                .build();
        sendPacket(channel, CmdId.REQ_COLLECT_FOOD, collectFood.toByteString());

        ReqMove moveReq = ReqMove.newBuilder()
                .setX(10.0f)
                .setY(20.0f)
                .build();
        sendPacket(channel, CmdId.REQ_MOVE, moveReq.toByteString());

        ReqBuildCreate buildCreate = ReqBuildCreate.newBuilder()
                .setBuildingType(1)
                .build();
        sendPacket(channel, CmdId.REQ_BUILDING_CREATE, buildCreate.toByteString());

        ReqBuildUpgrade buildUpgrade = ReqBuildUpgrade.newBuilder()
                .setBuildingType(1)
                .build();
        sendPacket(channel, CmdId.REQ_BUILDING_UPGRADE, buildUpgrade.toByteString());

        System.out.println("[发送] 全流程请求已发送（注册/登录/采集/移动/建筑创建/升级）");
    }

    private static void sendPacket(Channel channel, CmdId cmdId, ByteString content) {
        if (channel == null || !channel.isActive()) {
            System.out.println("[发送失败] 连接未建立或已断开，cmd=" + cmdId);
            return;
        }
        GamePacket packet = GamePacket.newBuilder()
                .setCmd(cmdId)
                .setContent(content)
                .setTimestamp(System.currentTimeMillis())
                .build();
        channel.writeAndFlush(packet).addListener(future -> {
            if (!future.isSuccess()) {
                System.out.println("[发送失败] cmd=" + cmdId + ", error=" + future.cause());
            } else {
                System.out.println("[发送成功] cmd=" + cmdId);
            }
        });
    }

    private static void printHelp() {
        System.out.println("============== 冬日测试客户端 ==============");
        System.out.println("help                                  显示帮助");
        System.out.println("register <username> <password>        注册");
        System.out.println("login <username> <password>           登录");
        System.out.println("move <x> <y>                           移动");
        System.out.println("collect <coal|wood|food> [amount]     采集资源");
        System.out.println("building_create <buildingType>        创建建筑");
        System.out.println("building_upgrade <buildingType>       升级建筑");
        System.out.println("all <username> <password>             一键测试全流程");
        System.out.println("quit|exit                             退出");
        System.out.println("======================================");
    }

    private static class ClientHandler extends SimpleChannelInboundHandler<GamePacket> {

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            System.out.println("[客户端] 已连接到服务器: " + ctx.channel().remoteAddress());
            super.channelActive(ctx);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, GamePacket msg) throws Exception {
            CmdId cmd = msg.getCmd();
            System.out.println("\n[响应] cmd=" + cmd);

            switch (cmd) {
                case RESP_LOGIN:
                    RespLogin loginResp = RespLogin.parseFrom(msg.getContent());
                    if (loginResp.getPlayerid() > 0) {
                        playerId = loginResp.getPlayerid();
                    }
                    System.out.println("  登录结果: code=" + loginResp.getCode() + ", msg=" + loginResp.getMsg() + ", playerId=" + loginResp.getPlayerid());
                    break;
                case RESP_REGISTER:
                    RespRegister regResp = RespRegister.parseFrom(msg.getContent());
                    System.out.println("  注册结果: code=" + regResp.getCode() + ", msg=" + regResp.getMsg() + ", playerId=" + regResp.getPlayerid());
                    break;
                case RESP_MOVE:
                    RespMove moveResp = RespMove.parseFrom(msg.getContent());
                    System.out.println("  移动结果: code=" + moveResp.getCode() + ", msg=" + moveResp.getMsg() + ", x=" + moveResp.getX() + ", y=" + moveResp.getY());
                    break;
                case RESP_BUILDING_CREATE:
                    RespBuildCreate createResp = RespBuildCreate.parseFrom(msg.getContent());
                    System.out.println("  建筑创建: code=" + createResp.getCode() + ", msg=" + createResp.getMsg() + ", type=" + createResp.getBuildingType() + ", level=" + createResp.getLevel());
                    break;
                case RESP_BUILDING_UPGRADE:
                    RespBuildUpgrade upgradeResp = RespBuildUpgrade.parseFrom(msg.getContent());
                    System.out.println("  建筑升级: code=" + upgradeResp.getCode() + ", msg=" + upgradeResp.getMsg() + ", type=" + upgradeResp.getBuildingType() + ", newLevel=" + upgradeResp.getNewLevel() + ", finishTime=" + upgradeResp.getUpgradeCompleteTime());
                    break;
                case RESP_COLLECT_COAL:
                case RESP_COLLECT_WOOD:
                case RESP_COLLECT_FOOD:
                    RespCollect collectResp = RespCollect.parseFrom(msg.getContent());
                    System.out.println("  采集结果: code=" + collectResp.getCode() + ", msg=" + collectResp.getMsg() + ", playerId=" + collectResp.getPlayerid());
                    break;
                default:
                    System.out.println("  未识别响应内容，长度=" + msg.getContent().size());
                    break;
            }

            System.out.print("请输入指令 > ");
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            System.out.println("\n[系统] 与服务器断开连接。");
            super.channelInactive(ctx);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            System.out.println("[客户端异常] " + cause.getMessage());
            cause.printStackTrace();
            ctx.close();
        }
    }
}
