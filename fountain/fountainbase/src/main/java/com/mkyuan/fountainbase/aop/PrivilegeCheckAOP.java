package com.mkyuan.fountainbase.aop;

import com.mkyuan.fountainbase.common.controller.response.ResponseBean;
import com.mkyuan.fountainbase.common.controller.response.ResponseCodeEnum;
import com.mkyuan.fountainbase.common.util.EncryptUtil;
import com.mkyuan.fountainbase.user.bean.UserLoginInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@Order(100)
public class PrivilegeCheckAOP {
    protected Logger logger = LogManager.getLogger(this.getClass());
    private final static String REDIS_USER_INFO="fountain:userinfo:";//保存一个UserLoginInfo的object，后跟token

    @Value("${fountain.secretKey}")
    private String secretKey = "";

    @Autowired
    private RedisTemplate redisTemplate;


    @Around("@annotation(com.mkyuan.fountainbase.aop.PrivilegeCheck)")
    public Object privilegeCheck(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            PrivilegeCheck annotation = method.getAnnotation(PrivilegeCheck.class);
            String[] parameterNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();
            String inputMyToken="";
            String inputUserName="";
            String validToken = "";
            for (int i = 0; i < parameterNames.length; i++) {
                //logger.info(">>>>>>aop parameterNames->{}",parameterNames[i]);
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
            String rediUserLoginInfoKey=REDIS_USER_INFO+validToken;
            Object obj=redisTemplate.opsForValue().get(rediUserLoginInfoKey);
            if(obj==null){
                return new ResponseBean(ResponseCodeEnum.NOPRIVILEGE_ERROR);
            }
            UserLoginInfo userLoginInfo=(UserLoginInfo) obj;
            if(userLoginInfo.getType()==3){
                return new ResponseBean(ResponseCodeEnum.NOPRIVILEGE_ERROR);
            }
        } catch (Exception e) {
            logger.error(">>>>>>privilege check aop error->{}", e);
            return new ResponseBean(ResponseCodeEnum.NOPRIVILEGE_ERROR);
        }
        return joinPoint.proceed();
    }
}
