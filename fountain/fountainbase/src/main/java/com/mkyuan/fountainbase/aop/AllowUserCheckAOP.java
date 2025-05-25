package com.mkyuan.fountainbase.aop;

import com.mkyuan.fountainbase.agent.chatbot.service.ChatService;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.lang.reflect.Method;

import com.alibaba.fastjson.JSONObject;

@Aspect
@Component
@Order(110)
public class AllowUserCheckAOP {
    protected Logger logger = LogManager.getLogger(this.getClass());
    private final static String REDIS_USER_INFO = "fountain:userinfo:";//保存一个UserLoginInfo的object，后跟token

    @Value("${fountain.secretKey}")
    private String secretKey = "";

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ChatService chatService;

    @Around("@annotation(com.mkyuan.fountainbase.aop.AllowUserCheck)")
    public Object allowUsersCheck(ProceedingJoinPoint joinPoint) throws Throwable {
        Class<?> returnType = ((MethodSignature) joinPoint.getSignature()).getReturnType();
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            AllowUserCheck annotation = method.getAnnotation(AllowUserCheck.class);
            String[] parameterNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();
            String inputMyToken = "";
            String inputUserName = "";
            JSONObject inputJsonParas = new JSONObject();
            long configMainId = 0l;
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
                if ("params".equals(parameterNames[i])) {
                    inputJsonParas = (JSONObject) args[i];
                    configMainId = inputJsonParas.getLong("configMainId");
                }
                if ("userName".equals(parameterNames[i])) { //header传过来有userName
                    inputUserName = String.valueOf(args[i]);
                }
            }
            String rediUserLoginInfoKey = REDIS_USER_INFO + validToken;
            Object obj = redisTemplate.opsForValue().get(rediUserLoginInfoKey);
            logger.info(">>>>>>在redis里搜索->" + rediUserLoginInfoKey);
            if (obj == null) {
                // 判断返回类型是否为SseEmitter
                if (SseEmitter.class.isAssignableFrom(returnType)) {
                    SseEmitter sseEmitter = new SseEmitter();
                    try {
                        sseEmitter.send("{\"data\":\"当前用户不允许参与该聊天\"}");
                        sseEmitter.complete();
                        return sseEmitter;
                    } catch (Exception e) {
                        logger.error("发送SSE消息失败", e);
                        throw new RuntimeException("发送SSE消息失败", e);
                    }
                } else {
                    // 其他类型返回ResponseBean
                    return new ResponseBean(ResponseCodeEnum.NOPRIVILEGE_ERROR.getCode(),
                                            "当前用户不允许参与该聊天");
                }
            }
            UserLoginInfo userLoginInfo = (UserLoginInfo) obj;
            logger.info(">>>>>>allowChat 检查前->userId->{} userName->{} configMainId->{}" + userLoginInfo.getUserId(),
                        userLoginInfo.getUserName(), configMainId);
            if (!this.chatService.allowChat(userLoginInfo.getUserId(), inputUserName, configMainId)) {
                // 判断返回类型是否为SseEmitter
                if (SseEmitter.class.isAssignableFrom(returnType)) {
                    SseEmitter sseEmitter = new SseEmitter();
                    try {
                        sseEmitter.send("{\"data\":\"当前用户不允许参与该聊天\"}");
                        sseEmitter.complete();
                        return sseEmitter;
                    } catch (Exception e) {
                        logger.error("发送SSE消息失败", e);
                        throw new RuntimeException("发送SSE消息失败", e);
                    }
                } else {
                    // 其他类型返回ResponseBean
                    return new ResponseBean(ResponseCodeEnum.NOPRIVILEGE_ERROR.getCode(),
                                            "当前用户不允许参与该聊天");
                }
            }
        } catch (Exception e) {
            logger.error(">>>>>>allow users check aop error->{}", e.getMessage(), e);
            // 判断返回类型是否为SseEmitter
            if (SseEmitter.class.isAssignableFrom(returnType)) {
                SseEmitter sseEmitter = new SseEmitter();
                try {
                    sseEmitter.send("{\"data\":\"当前用户不允许参与该聊天\"}");
                    sseEmitter.complete();
                    return sseEmitter;
                } catch (Exception se) {
                    logger.error("发送SSE消息失败", e);
                    throw new RuntimeException("发送SSE消息失败", e);
                }
            } else {
                // 其他类型返回ResponseBean
                return new ResponseBean(ResponseCodeEnum.NOPRIVILEGE_ERROR.getCode(),
                                        "当前用户不允许参与该聊天");
            }
        }
        return joinPoint.proceed();
    }
}
