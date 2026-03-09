package com.winter.modules.battle.core;

import com.alibaba.fastjson2.JSON;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 多人交互HTTP接口，挂载到已有的 BattleHttpTestServer 上。
 * 提供：登录/心跳、世界聊天、玩家位置同步 等接口，
 * 使多个浏览器窗口能看到彼此并实时聊天。
 */
public class MultiplayerHttpHandler {

    // ═══════════ 内存数据存储 ═══════════

    /** 在线玩家 { playerId -> PlayerInfo } */
    private static final ConcurrentHashMap<Long, PlayerInfo> onlinePlayers = new ConcurrentHashMap<>();

    /** 世界聊天消息（最近200条） */
    private static final CopyOnWriteArrayList<ChatMessage> worldChat = new CopyOnWriteArrayList<>();
    private static final int MAX_CHAT_HISTORY = 200;

    /** 简单的用户名 -> playerId 映射（模拟注册） */
    private static final ConcurrentHashMap<String, Long> userRegistry = new ConcurrentHashMap<>();
    private static long nextPlayerId = 10001;

    // ═══════════ 注册所有路由 ═══════════

    public static void register(HttpServer server) {
        server.createContext("/api/login", new LoginHandler());
        server.createContext("/api/heartbeat", new HeartbeatHandler());
        server.createContext("/api/chat/world", new WorldChatHandler());
        server.createContext("/api/players/nearby", new NearbyPlayersHandler());
        server.createContext("/api/move", new MoveHandler());
        System.out.println(">>> 多人交互HTTP接口已注册 (/api/login, /api/chat/world, /api/players/nearby, /api/move)");
    }

    // ═══════════ 登录接口 ═══════════

    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeCorsPreflight(exchange);
                return;
            }
            if (!"POST".equals(exchange.getRequestMethod())) {
                writeJson(exchange, 405, Map.of("error", "Method Not Allowed"));
                return;
            }

            byte[] body = exchange.getRequestBody().readAllBytes();
            Map<String, Object> req = JSON.parseObject(body);
            String username = req != null ? (String) req.get("username") : null;
            String password = req != null ? (String) req.get("password") : null;

            if (username == null || username.isBlank() || password == null || password.isBlank()) {
                writeJson(exchange, 400, Map.of("code", 1, "msg", "用户名和密码不能为空"));
                return;
            }

            // 简单注册/登录逻辑
            long playerId;
            synchronized (MultiplayerHttpHandler.class) {
                if (userRegistry.containsKey(username)) {
                    playerId = userRegistry.get(username);
                } else {
                    playerId = nextPlayerId++;
                    userRegistry.put(username, playerId);
                }
            }

            // 更新在线状态
            PlayerInfo info = onlinePlayers.computeIfAbsent(playerId, id -> new PlayerInfo());
            info.playerId = playerId;
            info.name = username;
            info.level = info.level == 0 ? 1 : info.level;
            info.lastHeartbeat = System.currentTimeMillis();
            // 首次登录分配随机位置
            if (info.x == 0 && info.y == 0) {
                info.x = 300 + (float) (Math.random() * 400);
                info.y = 200 + (float) (Math.random() * 400);
            }

            // 加入系统公告
            addWorldChat(0, "系统", username + " 进入了游戏世界！");

            writeJson(exchange, 200, Map.of(
                    "code", 0,
                    "msg", "登录成功",
                    "playerId", playerId,
                    "name", username,
                    "level", info.level,
                    "x", info.x,
                    "y", info.y
            ));
        }
    }

    // ═══════════ 心跳接口 ═══════════

    static class HeartbeatHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeCorsPreflight(exchange);
                return;
            }
            byte[] body = exchange.getRequestBody().readAllBytes();
            Map<String, Object> req = JSON.parseObject(body);
            long playerId = req != null ? ((Number) req.getOrDefault("playerId", 0)).longValue() : 0;
            PlayerInfo info = onlinePlayers.get(playerId);
            if (info != null) {
                info.lastHeartbeat = System.currentTimeMillis();
            }
            // 清理超时玩家（30秒无心跳）
            long now = System.currentTimeMillis();
            onlinePlayers.entrySet().removeIf(e -> now - e.getValue().lastHeartbeat > 30_000);

            writeJson(exchange, 200, Map.of("ok", true, "online", onlinePlayers.size()));
        }
    }

    // ═══════════ 世界聊天接口 ═══════════

    static class WorldChatHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeCorsPreflight(exchange);
                return;
            }

            if ("POST".equals(exchange.getRequestMethod())) {
                // 发送消息
                byte[] body = exchange.getRequestBody().readAllBytes();
                Map<String, Object> req = JSON.parseObject(body);
                long playerId = req != null ? ((Number) req.getOrDefault("playerId", 0)).longValue() : 0;
                String content = req != null ? (String) req.get("content") : null;

                if (content == null || content.isBlank()) {
                    writeJson(exchange, 400, Map.of("error", "消息内容不能为空"));
                    return;
                }
                // 限制消息长度
                if (content.length() > 200) {
                    content = content.substring(0, 200);
                }

                PlayerInfo sender = onlinePlayers.get(playerId);
                String senderName = sender != null ? sender.name : "未知玩家";

                addWorldChat(playerId, senderName, content);
                writeJson(exchange, 200, Map.of("ok", true));

            } else if ("GET".equals(exchange.getRequestMethod())) {
                // 拉取消息 — 支持 ?since=timestamp 只返回新消息
                String query = exchange.getRequestURI().getQuery();
                long since = 0;
                if (query != null) {
                    for (String param : query.split("&")) {
                        if (param.startsWith("since=")) {
                            try {
                                since = Long.parseLong(param.substring(6));
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }

                List<Map<String, Object>> msgs = new ArrayList<>();
                for (ChatMessage msg : worldChat) {
                    if (msg.timestamp > since) {
                        Map<String, Object> m = new HashMap<>();
                        m.put("playerId", msg.playerId);
                        m.put("sender", msg.sender);
                        m.put("content", msg.content);
                        m.put("timestamp", msg.timestamp);
                        msgs.add(m);
                    }
                }
                writeJson(exchange, 200, Map.of("messages", msgs));
            } else {
                writeJson(exchange, 405, Map.of("error", "Method Not Allowed"));
            }
        }
    }

    // ═══════════ 附近玩家接口 ═══════════

    static class NearbyPlayersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeCorsPreflight(exchange);
                return;
            }

            // GET /api/players/nearby?playerId=xxx
            String query = exchange.getRequestURI().getQuery();
            long playerId = 0;
            if (query != null) {
                for (String param : query.split("&")) {
                    if (param.startsWith("playerId=")) {
                        try {
                            playerId = Long.parseLong(param.substring(9));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }

            List<Map<String, Object>> players = new ArrayList<>();
            for (PlayerInfo info : onlinePlayers.values()) {
                if (info.playerId == playerId) continue;
                // 30秒内有心跳的才算在线
                if (System.currentTimeMillis() - info.lastHeartbeat > 30_000) continue;

                Map<String, Object> m = new HashMap<>();
                m.put("playerId", info.playerId);
                m.put("name", info.name);
                m.put("level", info.level);
                m.put("x", info.x);
                m.put("y", info.y);
                players.add(m);
            }
            writeJson(exchange, 200, Map.of("players", players));
        }
    }

    // ═══════════ 移动接口 ═══════════

    static class MoveHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeCorsPreflight(exchange);
                return;
            }
            if (!"POST".equals(exchange.getRequestMethod())) {
                writeJson(exchange, 405, Map.of("error", "Method Not Allowed"));
                return;
            }

            byte[] body = exchange.getRequestBody().readAllBytes();
            Map<String, Object> req = JSON.parseObject(body);
            long playerId = req != null ? ((Number) req.getOrDefault("playerId", 0)).longValue() : 0;
            float x = req != null ? ((Number) req.getOrDefault("x", 0)).floatValue() : 0;
            float y = req != null ? ((Number) req.getOrDefault("y", 0)).floatValue() : 0;

            PlayerInfo info = onlinePlayers.get(playerId);
            if (info != null) {
                info.x = x;
                info.y = y;
                info.lastHeartbeat = System.currentTimeMillis();
            }
            writeJson(exchange, 200, Map.of("ok", true));
        }
    }

    // ═══════════ 辅助 ═══════════

    private static void addWorldChat(long playerId, String sender, String content) {
        ChatMessage msg = new ChatMessage();
        msg.playerId = playerId;
        msg.sender = sender;
        msg.content = content;
        msg.timestamp = System.currentTimeMillis();
        worldChat.add(msg);
        // 保留最近的消息
        while (worldChat.size() > MAX_CHAT_HISTORY) {
            worldChat.remove(0);
        }
    }

    static class PlayerInfo {
        long playerId;
        String name;
        int level;
        float x, y;
        long lastHeartbeat;
    }

    static class ChatMessage {
        long playerId;
        String sender;
        String content;
        long timestamp;
    }

    private static void writeJson(HttpExchange exchange, int status, Object data) throws IOException {
        byte[] bytes = JSON.toJSONString(data).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static void writeCorsPreflight(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization");
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }
}
