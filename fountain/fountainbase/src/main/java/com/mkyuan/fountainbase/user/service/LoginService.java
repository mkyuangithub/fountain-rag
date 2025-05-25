package com.mkyuan.fountainbase.user.service;

import com.mkyuan.fountainbase.user.bean.LoginResult;

public interface LoginService {
    public LoginResult login(String userName, String password);

}
