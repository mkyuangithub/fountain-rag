package com.mkyuan.fountainbase.user.service;
import com.mkyuan.fountainbase.user.bean.LoginResult;

public abstract class AbstractLoginService implements LoginService {


    @Override
    public LoginResult login(String userName, String password) {
        // 通用的登录流程
        return doLogin( userName, password);
    }

    protected abstract LoginResult doLogin( String userName, String password);

}
