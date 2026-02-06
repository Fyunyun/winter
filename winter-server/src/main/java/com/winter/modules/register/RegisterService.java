package com.winter.modules.register;

import org.springframework.stereotype.Service;

@Service
public class RegisterService {

    private final RegisterDao registerDao;

    public RegisterService(RegisterDao registerDao) {
        this.registerDao = registerDao;
    }

    public int register(String username, String password) {
        return registerDao.register(username, password);
    }


}
