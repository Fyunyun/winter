package com.winter.modules.login;

import com.winter.common.model.PlayerModel;
import com.winter.core.router.GameHandler;
import com.winter.msg.MsgId.CmdId;

public class LoginController {

    LoginService loginService = new LoginService();

    @GameHandler(cmd = CmdId.REQ_LOGIN)
    public PlayerModel login(String username, String password) {
        return loginService.handleLogin(username, password);
    }
}
