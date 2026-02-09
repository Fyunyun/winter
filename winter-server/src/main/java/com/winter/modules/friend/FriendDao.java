package com.winter.modules.friend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.winter.core.db.DbManager;
import com.winter.modules.friend.Entry.FriendEntry;

import org.springframework.stereotype.Repository;

@Repository
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

    public boolean acceptFriendRequest(Long playerId, Long targetId) {
        String sql = "update friend set status = '1' " +
                "where (owner_id = ? and friend_id = ?) " +
                "   or (owner_id = ? and friend_id = ?)";

        try (Connection conn = DbManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);

            ps.setLong(1, playerId);
            ps.setLong(2, targetId);
            ps.setLong(3, targetId);
            ps.setLong(4, playerId);

            ps.executeUpdate();
            conn.commit();

            System.out.println("接受好友请求成功");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("接受好友请求失败");
        return false;
    }

    public boolean rejectFriendRequest(Long playerId, Long targetId) {
        String sql = "delete from friend where (owner_id = ? and friend_id = ?) " +
                "   or (owner_id = ? and friend_id = ?)";

        try (Connection conn = DbManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);

            ps.setLong(1, playerId);
            ps.setLong(2, targetId);
            ps.setLong(3, targetId);
            ps.setLong(4, playerId);

            ps.executeUpdate();

            conn.commit();
            // 在这里可以添加更多逻辑，比如更新好友状态等
            System.out.println("拒绝好友请求成功");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 在这里可以添加更多逻辑，比如更新好友状态等
        System.out.println("拒绝好友请求失败");
        return false;
    }

    public boolean hasRequest(Long playerId, Long targetId) {
        String sql = "select owner_id from friend where owner_id = ? and friend_id = ? and status = '0'";
        try (Connection conn = DbManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, playerId);
            ps.setLong(2, targetId);

            ResultSet executeQuery = ps.executeQuery();
            if (executeQuery.next()) {
                // 有好友请求
                System.out.println("有好友" + targetId + "请求");
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<FriendEntry> getFriendList(Long myId) {
        String sql = "select friend_id, status from friend where owner_id = ?";
        List<FriendEntry> friendIds = new ArrayList<>();
        try (Connection conn = DbManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, myId);

            ResultSet rs = ps.executeQuery();
            System.out.println("好友列表：");
            while (rs.next()) {
                Long friendId = rs.getLong("friend_id");
                int status = rs.getInt("status");
                friendIds.add(new FriendEntry(friendId, status));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return friendIds;
    }
}