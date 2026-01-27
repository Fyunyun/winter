package com.winter.modules.register;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.winter.core.db.DbManager;
import com.winter.msg.ErrorMsg.ErrorCode;
import com.winter.msg.SuccessMsg.SuccessCode;

public class RegisterDao {

    // 注册新账号
    public int register(String username, String password) {

        long newPlayerId = System.currentTimeMillis();

        String sqlAccount = "Insert INTO account (username, password, player_id) VALUES (?, ?, ?)";
        String sqlPlayer = "Insert INTO player_main (id, name, wood, coal, level, x, y) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String sqlBuilding = "Insert INTO player_building (player_id, building_type, level, status, finish_time) VALUES (?, ?, ?, ?, ?)";

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            return ErrorCode.REGISTER_USERNAME_OR_PASSWORD_EMPTY.getNumber();
        } else if (isUsernameExists(username)) {
            return ErrorCode.REGISTER_USER_ALREADY_EXISTS.getNumber();
        } else {
            try (Connection conn = DbManager.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    // 插入账号表
                    PreparedStatement psAccount = conn.prepareStatement(sqlAccount);
                    psAccount.setString(1, username);
                    psAccount.setString(2, password);
                    psAccount.setLong(3, newPlayerId);
                    psAccount.executeUpdate();

                    // 插入玩家主表
                    PreparedStatement psPlayer = conn.prepareStatement(sqlPlayer);
                    psPlayer.setLong(1, newPlayerId);
                    psPlayer.setString(2, username); // 默认角色名为用户名
                    psPlayer.setLong(3, 100); // 初始木材
                    psPlayer.setLong(4, 100); // 初始煤炭
                    psPlayer.setInt(5, 1); // 初始等级
                    psPlayer.setFloat(6, 0f); // 初始X坐标
                    psPlayer.setFloat(7, 0f); // 初始Y坐标
                    psPlayer.executeUpdate();

                    // 插入初始建筑数据 (熔炉, 兵营, 伐木场)
                    PreparedStatement psBuilding = conn.prepareStatement(sqlBuilding);
                    psBuilding.setLong(1, newPlayerId);
                    psBuilding.setInt(2, 1); // 熔炉
                    psBuilding.setInt(3, 1); // 等级1
                    psBuilding.setInt(4, 0); // 闲置
                    psBuilding.setLong(5, 0L); // 无结束时间
                    psBuilding.executeUpdate();

                    conn.commit();

                } catch (Exception e) {
                    conn.rollback();
                    e.printStackTrace();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return SuccessCode.REGISTER_SUCCESS.getNumber();
        }
    }

    public boolean isUsernameExists(String username) {
        String sql = "SELECT username FROM account WHERE username=?";
        try (Connection conn = DbManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
