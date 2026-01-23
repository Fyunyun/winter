package com.winter.modules.auth;

import com.winter.common.model.PlayerModel;
import com.winter.core.db.DataService;

public class LoginService {

    /**
     * 统一的登录入口
     * 
     * @return 登录成功返回 PlayerModel，失败返回 null (或者抛出异常)
     */
    public static PlayerModel handleLogin(String username, String password) {
        System.out.println("用户正在尝试登录: " + username);

        // 步骤 1: 验证身份 (这一步走 MySQL)
        long pid = DataService.LOGIN_VERIFY(username, password);

        if (pid <= 0) {
            System.out.println("登录失败：账号或密码错误");
            return null; // 或者 throw new RuntimeException("密码错误");
        }

        // 步骤 2: (可选) 检查账号是否被封号
        // if (BanManager.isBanned(pid)) { ... }

        // 步骤 3: 加载数据 (这一步优先走 Redis)
        PlayerModel player = DataService.loadPlayer(pid);

        if (player == null) {
            System.out.println("严重错误：账号存在但角色数据丢失！PID: " + pid);
            return null;
        }

        // 步骤 4: 后置处理 (比如更新 'last_login_time')
        // updateLoginTime(player);

        return player;
    }

    // private static void updateLoginTime(PlayerModel player) {
    // // 简单更新一下内存，等定时任务自动同步到 MySQL
    // player.setLastLogin(System.currentTimeMillis());
    // }
}