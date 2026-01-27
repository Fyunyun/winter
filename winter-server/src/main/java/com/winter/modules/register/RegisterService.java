package com.winter.modules.register;

public class RegisterService {

    RegisterDao registerDao = new RegisterDao();

    public int register(String username, String password) {
        return registerDao.register(username, password);
    }


}
