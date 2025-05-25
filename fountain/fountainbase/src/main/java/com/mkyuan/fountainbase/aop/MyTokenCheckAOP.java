package com.mkyuan.fountainbase.aop;

import com.mkyuan.fountainbase.common.controller.response.ResponseBean;
import com.mkyuan.fountainbase.common.controller.response.ResponseCodeEnum;
import com.mkyuan.fountainbase.common.util.EncryptUtil;
import jodd.util.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.aspectj.lang.ProceedingJoinPoint;

import java.lang.reflect.Method;

@Aspect
@Component
@Order(99)
public class MyTokenCheckAOP {
    protected Logger logger = LogManager.getLogger(this.getClass());
    private final static String validTokenRedisKeyPrefix = "fountain:userinfo:token:";
    private final static String REDIS_LOGIN_USER = "fountain:userinfo:userName:";
    @Value("${fountain.secretKey}")
    private String secretKey = "";

    @Autowired
    private RedisTemplate redisTemplate;


    @Around("@annotation(com.mkyuan.fountainbase.aop.MyTokenCheck)")
    public Object myTokenCheck(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            // 获取注解
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            MyTokenCheck annotation = method.getAnnotation(MyTokenCheck.class);
            String[] parameterNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();
            String inputMyToken="";
            String inputUserName="";
            String validToken = "";
            for (int i = 0; i < parameterNames.length; i++) {
                //logger.info(">>>>>>aop parameterNames->{}",parameterNames[i]);
                if ("userName".equals(parameterNames[i])) { //header传过来的token进行校验
                    inputUserName = new String(String.valueOf(args[i]));
                }
                if ("token".equals(parameterNames[i])) { //header传过来的token进行校验
                    inputMyToken = new String(String.valueOf(args[i]));
                    try {
                        validToken = EncryptUtil.decrypt_safeencode(inputMyToken, secretKey);
                    } catch (Exception e) {
                        logger.error(">>>>>>取得到的header里的token为空或者非法");
                        validToken = "";
                    }
                    //logger.info(">>>>>>AOP校验token,解密后的token为", validToken);
                }
            }
            //logger.info(">>>>>>input validToken->{} and inputUserName->{}",validToken,inputUserName);
            if(StringUtil.isBlank(validToken)||StringUtil.isBlank(inputUserName)){
                logger.info(">>>>>>token和userName为空，无效的登录");
                return new ResponseBean(ResponseCodeEnum.NOPRIVILEGE_ERROR);
            }

            if (this.isValidToken(validToken,inputUserName)) {
                return joinPoint.proceed();
            } else {
                return new ResponseBean(ResponseCodeEnum.NOPRIVILEGE_ERROR);
            }
            //return joinPoint.proceed();
        } catch (Exception e) {
            logger.error(">>>>>>检查用户的登录时token是否有效出错->{}", e.getMessage(), e);
            return new ResponseBean(ResponseCodeEnum.NOPRIVILEGE_ERROR);
        }
    }

    public boolean isValidToken(String token,String userName) {
        boolean flag = false;
        try {
            String validTokenRedis = validTokenRedisKeyPrefix + token;
            String validUserRedis=REDIS_LOGIN_USER+userName;
            Object tokenObj = redisTemplate.opsForValue().get(validTokenRedis);
            if (tokenObj == null) {
                logger.info(">>>>>>用户输入的token根本不存在，返回false");
                return false;
            }else{
                Object userTokenObj=redisTemplate.opsForValue().get(validUserRedis);
                if(userTokenObj==null){
                    logger.info(">>>>>>userName->{} 在redis->{}中不存在",validUserRedis);
                    return false;
                }else{
                    String existedUserToken=(String)userTokenObj;
                    if(existedUserToken.equals(token)){
                        return true;
                    }else{
                        logger.info(">>>>>>输入的token->{}与存在的token不相等",token,existedUserToken);
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            logger.error(">>>>>>校验token出错->{}", e.getMessage(), e);
        }
        return flag;
    }
}
