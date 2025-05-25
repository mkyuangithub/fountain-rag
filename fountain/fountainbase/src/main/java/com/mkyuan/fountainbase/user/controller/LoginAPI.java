package com.mkyuan.fountainbase.user.controller;

import com.alibaba.fastjson.JSONObject;
import com.mkyuan.fountainbase.aop.MyTokenCheck;
import com.mkyuan.fountainbase.aop.PrivilegeCheck;
import com.mkyuan.fountainbase.common.controller.response.ResponseBean;
import com.mkyuan.fountainbase.common.controller.response.ResponseCodeEnum;
import com.mkyuan.fountainbase.common.util.EncryptUtil;
import com.mkyuan.fountainbase.user.bean.LoginResult;
import com.mkyuan.fountainbase.user.service.LoginService;
import com.mkyuan.fountainbase.user.service.LoginServiceFactory;
import com.mkyuan.fountainbase.user.service.UserLocaleService;
import com.mkyuan.fountainbase.user.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
public class LoginAPI {
    protected Logger logger = LogManager.getLogger(this.getClass());

    @Value("${fountain.secretKey}")
    private String secretKey = "";


    @Autowired
    private LoginServiceFactory loginServiceFactory;

    @Autowired
    private UserService userService;

    @Autowired
    private UserLocaleService userLocaleService;

    @RequestMapping(value = "/api/user/logout", method = RequestMethod.POST)
    @ResponseBody
    public ResponseBean logout(@RequestHeader("token") String token,@RequestHeader("userName") String userName) {
        ResponseBean resp = new ResponseBean();
        try {
            String encryptedToken = token;
            String decryptedToken = "";
            try {
                decryptedToken = EncryptUtil.decrypt_safeencode(token, secretKey);
            } catch (Exception e) {
                decryptedToken = token;
            }
           this.userService.logout(userName,decryptedToken);
            return new ResponseBean(ResponseCodeEnum.SUCCESS,"logout");
        } catch (Exception e) {
            logger.error(">>>>>>user logout API error->{}", e.getMessage(), e);
        }
        return resp;
    }

    @RequestMapping(value = "/api/user/checkLoginToken", method = RequestMethod.POST)
    @ResponseBody
    public ResponseBean checkLoginToken(@RequestHeader("token") String token, @RequestBody JSONObject params) {
        ResponseBean resp = new ResponseBean();
        try {
            String encryptedToken = token;
            String decryptedToken = "";
            try {
                decryptedToken = EncryptUtil.decrypt_safeencode(token, secretKey);
            } catch (Exception e) {
                decryptedToken = token;
            }
            boolean result = this.userService.checkLoginToken(decryptedToken);
            if (result) {
                return new ResponseBean(ResponseCodeEnum.SUCCESS, true);
            } else {
                return new ResponseBean(ResponseCodeEnum.SUCCESS, false);
            }
        } catch (Exception e) {
            logger.error(">>>>>>check user token error->{}", e.getMessage(), e);
        }
        return resp;
    }

    @RequestMapping(value = "/api/user/login", method = RequestMethod.POST)
    @ResponseBody
    public ResponseBean login( @RequestBody JSONObject params) {

        ResponseBean resp = new ResponseBean();
        try {
            int loginType = 0;
            String userName = "";
            String encryptPassword = "";
            String decryptPassword = "";
            if (!params.containsKey("loginType")) {
                resp = new ResponseBean(ResponseCodeEnum.NOPRIVILEGE_ERROR);
                return resp;
            }
            if (!params.containsKey("userName")) {
                resp = new ResponseBean(ResponseCodeEnum.NOPRIVILEGE_ERROR);
                return resp;
            }
            if (!params.containsKey("password")) {
                resp = new ResponseBean(ResponseCodeEnum.NOPRIVILEGE_ERROR);
                return resp;
            }
            loginType = params.getInteger("loginType");
            encryptPassword = params.getString("password");
            userName = params.getString("userName");
            try {
                decryptPassword = EncryptUtil.decrypt_safeencode(encryptPassword, secretKey);
            } catch (Exception e) {
                //logger.error(">>>>>>Decrypt error->{}",e.getMessage(),e);
                decryptPassword = new String(encryptPassword);
            }

            LoginService loginService = this.loginServiceFactory.getLoginService(loginType);
            LoginResult loginResult = loginService.login(userName, decryptPassword);
            if (loginResult.getResult() == -1) {
                resp = new ResponseBean(ResponseCodeEnum.LOGIN_ERROR);
                return resp;
            } else if (loginResult.getResult() == 1) {
                resp = new ResponseBean(ResponseCodeEnum.SUCCESS.getCode(), "login success", loginResult);
                return resp;
            }
        } catch (Exception e) {
            resp = new ResponseBean(ResponseCodeEnum.FAIL);
        }
        return resp;
    }
    @RequestMapping(value = "/api/user/setLocale", method = RequestMethod.POST)
    @ResponseBody
    @MyTokenCheck
    @PrivilegeCheck
    public ResponseBean setLocale(@RequestHeader("token") String token, @RequestHeader("userName") String userName,@RequestBody JSONObject params) {
        try {
            String localeValue = params.getString("localeValue");

            if (StringUtils.isEmpty(localeValue) ||
                    (!localeValue.equals("zh") && !localeValue.equals("en"))) {
                return new ResponseBean(ResponseCodeEnum.ILLEGAL_PARAMETERS.getCode(),
                                        "Invalid locale value", null);
            }

            userLocaleService.setUserLocale(userName, localeValue);
            return new ResponseBean(ResponseCodeEnum.SUCCESS.getCode(),
                                    "Success", null);
        } catch (Exception e) {
            return new ResponseBean(ResponseCodeEnum.FAIL.getCode(),
                                    "Set locale failed", null);
        }
    }
}
