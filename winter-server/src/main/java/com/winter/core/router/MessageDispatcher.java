package com.winter.core.router;

import com.winter.common.model.PlayerModel;
import com.winter.modules.building.BuildingController;
import com.winter.modules.move.MoveController;
import com.winter.modules.login.LoginController;
import com.winter.modules.register.RegisterController;


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
        Method method;     // 处理方法 (如 upgrade)

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
        register(new BuildingController());
        
        // === 注册登录模块 ===
        register(new LoginController()); 

        // === 注册注册模块 ===
        register(new RegisterController());

        // == 注册移动模块 ==
        register(new MoveController());
        
        System.out.println("消息分发器初始化完成，注册了 " + HANDLER_MAP.size() + " 个路由。");
    }

    // 辅助方法：解析 Controller 里的注解
    private static void register(Object controller) {
        // 反射获取该类所有方法
        Method[] methods = controller.getClass().getDeclaredMethods();
        for (Method m : methods) {
            // 如果方法上有 @GameHandler 注解
            if (m.isAnnotationPresent(GameHandler.class)) {
                GameHandler annotation = m.getAnnotation(GameHandler.class);
                CmdId cmd = annotation.cmd();
                
                HANDLER_MAP.put(cmd, new HandlerDef(controller, m));
                System.out.println("路由注册: " + cmd + " -> " + controller.getClass().getSimpleName() + "." + m.getName());
            }
        }
    }

    /**
     * 2. 分发：Netty 收到包后调用这个方法
     */
    public static void dispatch(ChannelHandlerContext ctx, GamePacket packet) {
        CmdId cmd = packet.getCmd();
        HandlerDef def = HANDLER_MAP.get(cmd);

        if (def == null) {
            System.err.println("错误：未找到处理 CmdId=" + cmd + " 的方法");
            return;
        }

        try {
            // 获取当前玩家 (在登录时绑定的)
            // 如果是 REQ_LOGIN 这种不需要登录的包，player 可能为 null，Controller 里要判空
            PlayerModel player = (PlayerModel) ctx.channel().attr(AttributeKey.valueOf("PLAYER")).get();

            // === 核心反射调用 ===
            // 对应 BuildingController.upgrade(ctx, player, byte[])
            def.method.invoke(def.controller, ctx, player, packet.getContent().toByteArray());

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("业务逻辑执行出错: " + cmd);
        }
    }
}