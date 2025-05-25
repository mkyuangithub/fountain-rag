package com.mkyuan.fountainbase.user.service;

import com.mkyuan.fountainbase.common.util.MD5Util;
import com.mkyuan.fountainbase.user.bean.LoginResult;
import com.mkyuan.fountainbase.user.bean.UserInfo;
import com.mkyuan.fountainbase.user.bean.UserLoginInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;


@Service
public class PasswordLoginService extends AbstractLoginService {
    protected Logger logger = LogManager.getLogger(this.getClass());
    private final static String REDIS_USER_KEY = "fountain:userinfo:token:";
    private final static String REDIS_LOGIN_USER = "fountain:userinfo:userName:";
    private final static String REDIS_USER_INFO="fountain:userinfo:";//保存一个UserLoginInfo的object，后跟token

    @Value("${fountain.signature}")
    private String signature = "";

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    protected LoginResult doLogin(String userName, String password) {
        LoginResult loginResult = new LoginResult();
        logger.info(">>>>>>password after decrypt->{}", password);
        try {
            String collectionName = "UserInfo";
            String inputPasswordMd5 = MD5Util.getMD5(password);
            Query query = new Query(Criteria.where("userName").is(userName));
            UserInfo userInfo = this.mongoTemplate.findOne(query, UserInfo.class, collectionName);
            String returnToken="";

            if (userInfo != null) {
                String existedPassword = userInfo.getPassword();
                logger.info(">>>>>>decryptPassword->{} input password with md5->{} and existedPassword->{}", password, inputPasswordMd5, existedPassword);
                if (existedPassword.equals(inputPasswordMd5)) {

                    String existedRedisUserKey = REDIS_LOGIN_USER + userName;
                    Object obj = redisTemplate.opsForValue().get(existedRedisUserKey);
                    if (obj != null) {
                        returnToken = (String) obj;
                        loginResult = new LoginResult();
                        loginResult.setUserName(userName);
                        loginResult.setToken(returnToken);
                        loginResult.setUserId(userInfo.getId());
                        loginResult.setResult(1);
                    }else {
                        //开始生成myToken
                        returnToken = MD5Util.generateToken(userName, signature);
                        //store token into redis
                        String redisKey = REDIS_USER_KEY + returnToken;
                        redisTemplate.opsForValue().set(redisKey, returnToken);
                        redisTemplate.expire(redisKey, 15, TimeUnit.DAYS);
                        String redisUserKey = REDIS_LOGIN_USER + userName;
                        redisTemplate.opsForValue().set(redisUserKey, returnToken);
                        redisTemplate.expire(redisUserKey, 15, TimeUnit.DAYS);
                        //return the token to client side
                        loginResult = new LoginResult();
                        loginResult.setUserName(userInfo.getUserName());
                        loginResult.setResult(1);
                        loginResult.setToken(returnToken);
                        loginResult.setUserId(userInfo.getId());
                    }
                    //组装准备写入fountain:userinfo:token的对象
                    String userInfoKey=REDIS_USER_INFO+returnToken;
                    UserLoginInfo userLoginInfo=new UserLoginInfo(userInfo.getId(),userInfo.getUserName(),userInfo.getType());
                    redisTemplate.opsForValue().set(userInfoKey,userLoginInfo);//写入redis
                    return loginResult;
                }
            }
        } catch (Exception e) {
            logger.error(">>>>>> doLogin error->{}", e.getMessage(), e);
            loginResult = new LoginResult();
            loginResult.setResult(-1);
            return loginResult;
        }
        // 实现密码登录逻辑
        return new LoginResult();
    }
}
