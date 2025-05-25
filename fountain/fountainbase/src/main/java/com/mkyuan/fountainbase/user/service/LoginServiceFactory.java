package com.mkyuan.fountainbase.user.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginServiceFactory {
    private final Map<Integer, LoginService> loginServiceMap = new ConcurrentHashMap<>();

    @Autowired
    public LoginServiceFactory(PasswordLoginService passwordLoginService,
                               WechatLoginService wechatLoginService) {
        loginServiceMap.put(1, passwordLoginService);
        loginServiceMap.put(2, wechatLoginService);
    }

    public LoginService getLoginService(int loginType) {
        LoginService loginService = loginServiceMap.get(loginType);
        if (loginService == null) {
            throw new IllegalArgumentException("Unsupported login type: " + loginType);
        }
        return loginService;
    }
}
