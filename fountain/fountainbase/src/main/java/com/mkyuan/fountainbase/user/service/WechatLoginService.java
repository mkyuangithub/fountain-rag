package com.mkyuan.fountainbase.user.service;

import com.mkyuan.fountainbase.user.bean.LoginResult;
import org.springframework.stereotype.Service;

@Service
public class WechatLoginService extends AbstractLoginService {
    @Override
    protected LoginResult doLogin( String userName, String password) {
        // 实现密码登录逻辑
        return new LoginResult();
    }

}
