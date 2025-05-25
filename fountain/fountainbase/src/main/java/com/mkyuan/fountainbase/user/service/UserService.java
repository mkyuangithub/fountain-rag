package com.mkyuan.fountainbase.user.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    protected Logger logger = LogManager.getLogger(this.getClass());
    private final static String REDIS_USER_KEY = "fountain:userinfo:token:";
    private final static String REDIS_LOGIN_USER = "fountain:userinfo:userName:";
    private final static String REDIS_USER_INFO="fountain:userinfo:";//保存一个UserLoginInfo的object，后跟token
    @Autowired
    private RedisTemplate redisTemplate;

    public void logout(String userName, String decryptedToken){
        try{
            String userRedisKey=REDIS_LOGIN_USER+userName;
            String tokenRedisKey=REDIS_USER_KEY+decryptedToken;
            String userLoginInfoRedisKey=REDIS_USER_INFO+decryptedToken;
            Object userObj=redisTemplate.opsForValue().get(userRedisKey);
            if(userObj!=null){
                String loginedToken=(String)userObj;
                if(loginedToken.equals(decryptedToken)){
                    redisTemplate.delete(userRedisKey);
                    redisTemplate.delete(tokenRedisKey);
                    redisTemplate.delete(userLoginInfoRedisKey);
                }
            }
        }catch(Exception e){
            logger.error(">>>>>>logout error->{}",e.getMessage(),e);
        }
    }
    /**
     * check is a token has been logined
     * @param decryptedToken
     * @return -1-not login 1-login
     */
    public boolean checkLoginToken(String decryptedToken) {
        boolean result = false;
        try {
            String redisKey = REDIS_USER_KEY + decryptedToken;
            Object obj = redisTemplate.opsForValue().get(redisKey);
            if (obj == null) {
                return false;
            } else {
                result = true;
            }
        } catch (Exception e) {
            logger.error(">>>>>>checkLoginToken service error->{}", e.getMessage(), e);
        }
        return result;
    }
}
