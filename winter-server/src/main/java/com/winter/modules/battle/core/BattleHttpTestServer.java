package com.winter.modules.battle.core;

import com.alibaba.fastjson2.JSON;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.winter.modules.battle.config.BuffConfigTable;
import com.winter.modules.battle.config.SkillConfigTable;
import com.winter.modules.battle.config.SkillFactory;
import com.winter.modules.battle.model.BattleAction;
import com.winter.modules.battle.model.BattleEffectConfig;
import com.winter.modules.battle.model.BattleGroup;
import com.winter.modules.battle.model.BattleResult;
import com.winter.modules.battle.model.BattleUnit;
import com.winter.modules.battle.model.EffectType;
import com.winter.modules.battle.model.SoldiersType;
import com.winter.modules.battle.model.skill.Buff;
import com.winter.modules.battle.model.skill.GeneralSkill;
import com.winter.modules.battle.model.skill.SkillTrigger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BattleHttpTestServer {
    private final int port;
    private HttpServer server;

    public BattleHttpTestServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) throws IOException {
        // 加载配置表
        BuffConfigTable.getInstance().load("config/buff_config.json");
        SkillConfigTable.getInstance().load("config/skill_config.json");

        BattleHttpTestServer server = new BattleHttpTestServer(18088);
        server.start();
    }

    public synchronized void start() throws IOException {
        if (this.server != null) {
            return;
        }

        HttpServer httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.createContext("/api/battle/simulate", new SimulateHandler());
        httpServer.createContext("/health", exchange -> writeJson(exchange, 200, Map.of("ok", true)));
        httpServer.setExecutor(null);
        httpServer.start();

        this.server = httpServer;
        System.out.println("BattleHttpTestServer started at http://localhost:" + port);
    }

    public synchronized void stop() {
        if (this.server != null) {
            this.server.stop(0);
            this.server = null;
            System.out.println("BattleHttpTestServer stopped");
        }
    }

    public int getPort() {
        return port;
    }

    static class SimulateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeCorsPreflight(exchange);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeJson(exchange, 405, Map.of("error", "Method Not Allowed"));
                return;
            }

            byte[] body = exchange.getRequestBody().readAllBytes();
            SimulateRequest req = JSON.parseObject(body, SimulateRequest.class);
            if (req == null || req.attacker == null || req.defender == null) {
                writeJson(exchange, 400, Map.of("error", "Invalid request body"));
                return;
            }

            BattleGroup attacker = new BattleGroup("attacker");
            BattleGroup defender = new BattleGroup("defender");

            BattleUnit atk = buildUnit(req.attacker);
            BattleUnit def = buildUnit(req.defender);

            bindSkills(atk, req.attacker, true);
            bindSkills(def, req.defender, false);

            attacker.addUnit(atk);
            defender.addUnit(def);

            long seed = req.seed == 0 ? System.currentTimeMillis() : req.seed;
            int maxRounds = req.maxRounds <= 0 ? 100 : req.maxRounds;
            BattleEngine engine = new BattleEngine(attacker, defender, seed, maxRounds);
            BattleResult result = engine.simulate();

            Map<String, Object> resp = new HashMap<>();
            resp.put("seed", result.getSeed());
            resp.put("win", result.isWin());
            resp.put("logs", result.getBattleLogs());
            resp.put("actions", result.getActions());
            resp.put("attackerHp", atk.getHp());
            resp.put("defenderHp", def.getHp());
            resp.put("rounds", calcRounds(result.getActions()));

            // 返回攻防双方的技能列表（已配置的技能名称信息）
            resp.put("attackerSkills", buildSkillInfo(atk));
            resp.put("defenderSkills", buildSkillInfo(def));
            // 返回攻防双方的最终 Buff 状态
            resp.put("attackerBuffs", buildBuffInfo(atk));
            resp.put("defenderBuffs", buildBuffInfo(def));

            writeJson(exchange, 200, resp);
        }

        private static int calcRounds(List<BattleAction> actions) {
            int rounds = 0;
            for (BattleAction action : actions) {
                rounds = Math.max(rounds, action.getRound());
            }
            return rounds;
        }

        private static BattleUnit buildUnit(UnitRequest req) {
            BattleUnit unit = new BattleUnit();
            unit.setId(req.id);
            unit.setType(parseType(req.type));
            unit.setMaxHp(req.hp);
            unit.setHp(req.hp);
            unit.setBaseAtk(req.atk);
            unit.setBaseDef(req.def);
            return unit;
        }

        private static void bindSkills(BattleUnit unit, UnitRequest req, boolean attackerSide) {
            // 优先使用 skillIds 从配置表加载
            if (req.skillIds != null && !req.skillIds.isEmpty()) {
                List<GeneralSkill> skills = SkillFactory.createAll(req.skillIds);
                for (GeneralSkill skill : skills) {
                    unit.addSkill(skill);
                }
                return;
            }

            // 兼容旧的硬编码方式
            if (req.skillModPercent != 0) {
                unit.addSkill(new GeneralSkill(
                        attackerSide ? 1001 : 2001,
                        SkillTrigger.BEFORE_ATTACK,
                        1.0,
                        0,
                        null,
                        List.of(new BattleEffectConfig(EffectType.MOD_DAMAGE, req.skillModPercent, 0))
                ));
            }
            if (req.skillVampirePercent != 0) {
                unit.addSkill(new GeneralSkill(
                        attackerSide ? 1002 : 2002,
                        SkillTrigger.AFTER_ATTACK,
                        1.0,
                        0,
                        null,
                        List.of(new BattleEffectConfig(EffectType.VAMPIRE, req.skillVampirePercent, 0))
                ));
            }
            if (req.skillHeal > 0) {
                unit.addSkill(new GeneralSkill(
                        attackerSide ? 1003 : 2003,
                        SkillTrigger.ON_DAMAGED,
                        1.0,
                        0,
                        null,
                        List.of(new BattleEffectConfig(EffectType.HEAL, req.skillHeal, 0))
                ));
            }
            if (req.skillBuffTypeId > 0) {
                unit.addSkill(new GeneralSkill(
                        attackerSide ? 1004 : 2004,
                        SkillTrigger.BEFORE_ATTACK,
                        1.0,
                        1,
                        null,
                        List.of(new BattleEffectConfig(EffectType.ADD_BUFF, req.skillBuffTypeId,
                                req.skillBuffValue == 0 ? 10 : req.skillBuffValue))
                ));
            }
            if (req.skillReduceCd > 0) {
                unit.addSkill(new GeneralSkill(
                        attackerSide ? 1005 : 2005,
                        SkillTrigger.AFTER_ATTACK,
                        1.0,
                        2,
                        null,
                        List.of(new BattleEffectConfig(EffectType.REDUCE_CD, req.skillReduceCd, 0))
                ));
            }
        }

        private static List<Map<String, Object>> buildSkillInfo(BattleUnit unit) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (GeneralSkill skill : unit.getSkills()) {
                Map<String, Object> m = new HashMap<>();
                m.put("skillId", skill.getSkillId());
                var cfg = SkillConfigTable.getInstance().get(skill.getSkillId());
                m.put("name", cfg != null ? cfg.getName() : "技能#" + skill.getSkillId());
                m.put("description", cfg != null ? cfg.getDescription() : "");
                list.add(m);
            }
            return list;
        }

        private static List<Map<String, Object>> buildBuffInfo(BattleUnit unit) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (Buff buff : unit.getBuffs()) {
                Map<String, Object> m = new HashMap<>();
                m.put("buffId", buff.getBuffId());
                m.put("value", buff.getValue());
                m.put("rounds", buff.getRounds());
                var cfg = buff.getConfig();
                m.put("name", cfg != null ? cfg.getName() : "Buff#" + buff.getBuffId());
                m.put("positive", cfg != null && cfg.isPositive());
                list.add(m);
            }
            return list;
        }

        private static SoldiersType parseType(String type) {
            if (type == null) {
                return SoldiersType.INFANTRY;
            }
            try {
                return SoldiersType.valueOf(type.toUpperCase());
            } catch (Exception e) {
                return SoldiersType.INFANTRY;
            }
        }
    }

    static class SimulateRequest {
        public long seed;
        public int maxRounds;
        public UnitRequest attacker;
        public UnitRequest defender;
    }

    static class UnitRequest {
        public int id;
        public String type;
        public int hp;
        public int atk;
        public int def;

        // 新方式：直接传配置表中的 skillId 列表
        public List<Integer> skillIds;

        // 旧方式（兼容）
        public int skillModPercent;
        public int skillVampirePercent;
        public int skillHeal;
        public int skillBuffTypeId;
        public int skillBuffValue;
        public int skillReduceCd;
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
