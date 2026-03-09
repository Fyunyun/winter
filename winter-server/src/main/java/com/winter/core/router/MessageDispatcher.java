package com.winter.core.router;

import com.winter.common.model.PlayerModel;
import com.winter.modules.building.BuildingController;
import com.winter.modules.move.MoveController;
import com.winter.modules.login.LoginController;
import com.winter.modules.register.RegisterController;
import com.winter.modules.collect.CollectController;
import com.winter.modules.friend.FriendController;
import com.winter.modules.chat.ChatController;

import com.winter.core.spring.SpringContext;
import com.winter.core.util.GameExecutor;

import com.winter.msg.MsgId.CmdId;
import com.winter.msg.PacketMsg.GamePacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class MessageDispatcher {

    // 存储映射关系：CmdId -> {对象, 方法}
    private static final Map<CmdId, HandlerDef> HANDLER_MAP = new HashMap<>();

    // 内部类，定义一个处理器包括什么
    private static class HandlerDef {
        Object controller; // 控制器实例 (如 BuildingController)
        Method method; // 处理方法 (如 upgrade)

        public HandlerDef(Object controller, Method method) {
            this.controller = controller;
            this.method = method;
        }
    }

    /**
     * 1. 初始化：扫描并注册所有 Handler
     * (实际项目可以用 Spring 或 ClassScanner 自动扫描，这里为了简单手动注册)
     */
    public static void init() {
        // === 注册建筑模块 ===
        register(SpringContext.getBean(BuildingController.class));

        // === 注册登录模块 ===
        register(SpringContext.getBean(LoginController.class));

        // === 注册注册模块 ===
        register(SpringContext.getBean(RegisterController.class));

        // === 注册移动模块 ===
        register(SpringContext.getBean(MoveController.class));

        // === 注册好友模块 ===
        register(SpringContext.getBean(FriendController.class));

        // === 注册采集模块 ===
        register(SpringContext.getBean(CollectController.class));

        // === 注册聊天模块 ===
        register(SpringContext.getBean(ChatController.class));

        System.out.println("消息分发器初始化完成，注册了 " + HANDLER_MAP.size() + " 个路由。");
    }

    // 辅助方法：解析 Controller 里的注解
    private static void register(Object controller) { // 定义注册方法：把一个 controller 中标注了 @GameHandler 的方法注册到路由表
        Method[] methods = controller.getClass().getDeclaredMethods(); // 通过反射获取该 controller 类中声明的所有方法（不包含父类方法）
        for (Method m : methods) { // 遍历每一个方法
            if (m.isAnnotationPresent(GameHandler.class)) { // 判断该方法上是否标注了 @GameHandler 注解
                GameHandler annotation = m.getAnnotation(GameHandler.class); // 获取该方法上的 @GameHandler 注解实例
                for (CmdId cmd : annotation.cmd()) { // 遍历注解中声明的所有 CmdId（一个方法可绑定多个消息号）
                    HANDLER_MAP.put(cmd, new HandlerDef(controller, m)); // 将 CmdId 映射到 {controller实例, method}，用于后续分发时反射调用
                }
            }
        }
    }

    /**
     * 2. 分发：Netty 收到包后调用这个方法
     * 
     * 优化：将业务逻辑提交到独立的业务线程池执行，
     * 避免阻塞 Netty I/O 线程（敏感词过滤、Redis 读写等都是耗时操作）。
     * 登录和注册仍在 I/O 线程执行（需要即时绑定 Channel 属性）。
     */
    public static void dispatch(ChannelHandlerContext ctx, GamePacket packet) {
        CmdId cmd = packet.getCmd();
        System.out.println("[路由] 收到请求 cmd=" + cmd);
        HandlerDef def = HANDLER_MAP.get(cmd);
        if (def == null) {
            System.err.println("错误：未找到处理 CmdId=" + cmd + " 的方法");
            return;
        }

        // 登录/注册需要在 I/O 线程同步执行（绑定 Channel Attribute）
        if (cmd == CmdId.REQ_LOGIN || cmd == CmdId.REQ_REGISTER) {
            invokeHandler(ctx, packet, cmd, def);
        } else {
            // 其他业务逻辑提交到业务线程池，释放 I/O 线程
            GameExecutor.execute(() -> invokeHandler(ctx, packet, cmd, def));
        }
    }

    /**
     * 实际执行 Handler 反射调用
     */
    private static void invokeHandler(ChannelHandlerContext ctx, GamePacket packet, CmdId cmd, HandlerDef def) {
        try {
            PlayerModel player = (PlayerModel) ctx.channel().attr(AttributeKey.valueOf("PLAYER")).get();

            if (def.method.getParameterCount() == 3) {
                def.method.invoke(def.controller, ctx, player, packet.getContent().toByteArray());
            } else if (def.method.getParameterCount() == 4) {
                def.method.invoke(def.controller, ctx, player, packet.getContent().toByteArray(), cmd);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("业务逻辑执行出错: " + cmd);
        }
    }
}