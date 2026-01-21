package com.winter;

import com.winter.db.DataService;
import com.winter.model.PlayerModel;

public class LoginTest {
    public static void main(String[] args) {
    System.out.println("注册结果: " + DataService.register("winter_001", "pwd123"));

    // 2. 尝试登录
    long pid = DataService.LOGIN_VERIFY("winter_001", "pwd123");
    if (pid > 0) {
        System.out.println("登录成功！获得 PlayerID: " + pid);
        // 3. 接着加载资源
        PlayerModel model = DataService.loadPlayer(pid);
        System.out.println("加载木材: " + model.getWood());
    } else {
        System.out.println("登录失败！");
        }
    }
}