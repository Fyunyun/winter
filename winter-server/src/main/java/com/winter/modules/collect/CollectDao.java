package com.winter.modules.collect;
import com.winter.common.model.PlayerModel;
import com.winter.core.db.DbManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CollectDao {

    public boolean collectCoal(PlayerModel player, int amount) {
        int id = (int) player.getPlayerId();
        // 数据库操作：增加玩家的煤炭资源
        String sql = "UPDATE player_main SET coal = coal + ? WHERE id = ?";
        try (Connection conn = DbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, amount);
            ps.setInt(2, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean collectWood(PlayerModel player, int amount) {
        int id = (int) player.getPlayerId();
        // 数据库操作：增加玩家的木材资源
        String sql = "UPDATE player_main SET wood = wood + ? WHERE id = ?";
        try (Connection conn = DbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, amount);
            ps.setInt(2, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean collectFood(PlayerModel player, int amount) {
        int id = (int) player.getPlayerId();
        // 数据库操作：增加玩家的食物资源
        String sql = "UPDATE player_main SET food = food + ? WHERE id = ?";
        try (Connection conn = DbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, amount);
            ps.setInt(2, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

}
