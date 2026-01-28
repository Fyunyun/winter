package com.winter;

import com.winter.common.model.BuildingModel;
import com.winter.common.model.PlayerModel;
import com.winter.core.db.DbManager;
import com.winter.modules.building.BuildingDao;
import com.winter.modules.building.BuildingService;
import com.winter.modules.collect.CollectService;
import com.winter.modules.login.LoginDao;
import com.winter.modules.move.MoveService;
import com.winter.modules.register.RegisterService;
import com.winter.msg.ErrorMsg.ErrorCode;
import com.winter.msg.SuccessMsg.SuccessCode;

import redis.clients.jedis.Jedis;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class FullIntegrationTest {

    public static void main(String[] args) {
        System.out.println("========== Full Integration Test (MySQL + Redis) ==========");

        String username = "test_user_" + System.currentTimeMillis();
        String password = "pwd123";

        Long playerId = null;

        try {
            // 1) Register
            RegisterService registerService = new RegisterService();
            int registerCode = registerService.register(username, password);
            if (registerCode != SuccessCode.REGISTER_SUCCESS.getNumber()) {
                System.err.println("Register failed: code=" + registerCode + " (" + resolveRegisterMsg(registerCode) + ")");
                return;
            }
            System.out.println("[Register] success: " + username);

            playerId = getPlayerIdByUsername(username);
            if (playerId == null) {
                System.err.println("[Register] cannot find playerId for username=" + username);
                return;
            }
            System.out.println("[Register] playerId=" + playerId);

            // 2) Login (DAO verify + load)
            LoginDao loginDao = new LoginDao();
            long pid = loginDao.LOGIN_VERIFY(username);
            if (pid <= 0) {
                System.err.println("[Login] LOGIN_VERIFY failed");
                return;
            }
            boolean passwordOk = loginDao.VERIFY_PASSWORD(username, password);
            if (!passwordOk) {
                System.err.println("[Login] VERIFY_PASSWORD failed");
                return;
            }
            PlayerModel player = loginDao.loadPlayer(pid);
            if (player == null) {
                System.err.println("[Login] loadPlayer failed");
                return;
            }
            System.out.println("[Login] success: " + player.getPlayerId() + ", wood=" + player.getWood());

            // 3) Collect (coal/wood/food)
            CollectService collectService = new CollectService();
            collectService.collectCoal(player, 5);
            collectService.collectWood(player, 8);
            collectService.collectFood(player, 3);
            System.out.println("[Collect] coal+5 wood+8 food+3");
            printPlayerMain(player.getPlayerId());

            // 4) Move (update redis hash + geo)
            MoveService moveService = new MoveService();
            boolean moved = moveService.movePlayer(player, 12.5f, 20.0f);
            System.out.println("[Move] moved=" + moved);
            checkRedisPosition(player.getPlayerId());

            // 5) Building (get/upgrade/complete)
            BuildingService buildingService = new BuildingService();
            BuildingModel building = buildingService.getBuilding(player.getPlayerId(), 1);
            System.out.println("[Building] current level=" + building.getLevel() + ", status=" + building.getStatus());

            int upgradeCode = buildingService.upgradeBuilding(player, 1);
            System.out.println("[Building] upgrade result=" + upgradeCode);

            // Force completion by setting finish_time in past
            BuildingDao buildingDao = new BuildingDao();
            BuildingModel updated = buildingService.getBuilding(player.getPlayerId(), 1);
            updated.setStatus(1);
            updated.setFinishTime(System.currentTimeMillis() - 1000);
            buildingDao.saveBuildingToRedis(player.getPlayerId(), updated);
            buildingDao.saveBuildingToMysql(player.getPlayerId(), updated);

            boolean completed = buildingService.completeBuildingUpgrade(player.getPlayerId(), 1);
            System.out.println("[Building] complete result=" + completed);
            BuildingModel after = buildingService.getBuilding(player.getPlayerId(), 1);
            System.out.println("[Building] after level=" + after.getLevel() + ", status=" + after.getStatus());

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (playerId != null) {
                cleanup(playerId, username);
            }
        }

        System.out.println("========== Test Finished ==========");
    }

    private static String resolveRegisterMsg(int code) {
        if (code == ErrorCode.REGISTER_USERNAME_OR_PASSWORD_EMPTY.getNumber()) {
            return "用户名或密码为空";
        }
        if (code == ErrorCode.REGISTER_USER_ALREADY_EXISTS.getNumber()) {
            return "用户已存在";
        }
        return "未知错误";
    }

    private static Long getPlayerIdByUsername(String username) {
        String sql = "SELECT player_id FROM account WHERE username=?";
        try (Connection conn = DbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("player_id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void printPlayerMain(long playerId) {
        String sql = "SELECT wood, coal, food, level, x, y FROM player_main WHERE id=?";
        try (Connection conn = DbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("[MySQL] wood=" + rs.getLong("wood") +
                            ", coal=" + rs.getLong("coal") +
                            ", food=" + rs.getLong("food") +
                            ", level=" + rs.getInt("level") +
                            ", x=" + rs.getFloat("x") +
                            ", y=" + rs.getFloat("y"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void checkRedisPosition(long playerId) {
        try (Jedis jedis = DbManager.getJedis()) {
            String key = "p:data:" + playerId;
            System.out.println("[Redis] p:data exists=" + jedis.exists(key));
            System.out.println("[Redis] p:data=" + jedis.hgetAll(key));
            System.out.println("[Redis] geo pos=" + jedis.geopos("world:map:pos", String.valueOf(playerId)));
        }
    }

    private static void cleanup(long playerId, String username) {
        System.out.println("[Cleanup] playerId=" + playerId + ", username=" + username);

        try (Connection conn = DbManager.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM player_building WHERE player_id=?")) {
                ps.setLong(1, playerId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM player_main WHERE id=?")) {
                ps.setLong(1, playerId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM account WHERE username=?")) {
                ps.setString(1, username);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try (Jedis jedis = DbManager.getJedis()) {
            jedis.del("p:data:" + playerId);
            jedis.del("p:build:" + playerId);
            jedis.zrem("world:map:pos", String.valueOf(playerId));
        }
    }
}
