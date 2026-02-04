package com.winter.modules.friend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import com.winter.core.db.DbManager;

public class FriendDao {

    public boolean isFriend(Long playerId, Long friendId) {
        String sql = "select owner_id from friend where owner_id = ? and friend_id = ?";
        try (Connection conn = DbManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, playerId);
            ps.setLong(2, friendId);

            ResultSet executeQuery = ps.executeQuery();
            if (executeQuery.next()) {
                // 已经是好友关系
                System.out.println("已经是好友关系，playerId=" + playerId + ", friendId=" + friendId);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean addFriendRequest(Long playerId, Long friendId) {
        String sql = "insert into friend (owner_id, friend_id) values (?, ?), (?, ?)";
        try (Connection conn = DbManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, playerId);
            ps.setLong(2, friendId);
            ps.setLong(3, friendId);
            ps.setLong(4, playerId);

            ps.executeUpdate();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
