package com.mkyuan.fountainbase.agent.chatbot.controller;

import com.alibaba.fastjson.JSONObject;
import com.mkyuan.fountainbase.agent.chatbot.bean.UserChatConfigBean;
import com.mkyuan.fountainbase.agent.chatbot.service.ChatConfigTool;
import com.mkyuan.fountainbase.agent.chatbot.service.ChatRunningStatus;
import com.mkyuan.fountainbase.agent.chatbot.service.ChatService;
import com.mkyuan.fountainbase.aop.AllowUserCheck;
import com.mkyuan.fountainbase.aop.MyTokenCheck;
import com.mkyuan.fountainbase.aop.PrivilegeCheck;
import com.mkyuan.fountainbase.common.controller.response.ResponseBean;
import com.mkyuan.fountainbase.common.controller.response.ResponseCodeEnum;
import com.mkyuan.fountainbase.common.util.EncryptUtil;
import com.mkyuan.fountainbase.user.bean.UserLoginInfo;
import jodd.util.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class ChatApi {
    protected Logger logger = LogManager.getLogger(this.getClass());

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ChatService chatService;

    @Autowired
    private ChatRunningStatus chatRunningStatus;

    @MyTokenCheck
    @RequestMapping(value = "/api/ai/chat/initSession", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    @AllowUserCheck
    public ResponseBean initSession(@RequestHeader("token") String token, @RequestHeader("userName") String userName,
                                    @RequestBody JSONObject params) {
        long configMainId = params.getLong("configMainId");
        this.chatService.cleanLocalSession(userName);
        this.chatService.cleanSession(userName,configMainId);
        return new ResponseBean(ResponseCodeEnum.SUCCESS);
    }


    @MyTokenCheck
    @RequestMapping(value = "/api/ai/chat/hasMoreData", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    @AllowUserCheck
    public ResponseBean hasMoreData(@RequestHeader("token") String token, @RequestHeader("userName") String userName,
                                    @RequestBody JSONObject params) {
        ResponseBean responseBean = new ResponseBean();
        boolean result = this.chatService.hasMoreData(userName);
        return new ResponseBean(ResponseCodeEnum.SUCCESS, result);
    }

    @MyTokenCheck
    @RequestMapping(value = "/api/ai/chat/stopChat", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean stopChat(@RequestHeader("token") String token, @RequestHeader("userName") String userName,
                                 @RequestBody JSONObject params) {
        ResponseBean responseBean = new ResponseBean();
        this.chatRunningStatus.setRunning(userName, false);
        logger.info(">>>>>>用户主动发出聊天终止信号");
        return new ResponseBean(ResponseCodeEnum.SUCCESS, false);
    }

    @MyTokenCheck
    @RequestMapping(value = "/api/ai/chat/userChat", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    @AllowUserCheck
    public SseEmitter userChat(@RequestHeader("token") String token, @RequestHeader("userName") String userName,
                               @RequestBody JSONObject params) {
        SseEmitter sseEmitter = new SseEmitter(0L);
        try {
            String imgBase64Data = "";
            int topK = this.chatService.DEFAULT_TOPK;
            String inputPrompt = params.getString("inputPrompt");
            long configMainId = params.getLong("configMainId");
            String conversationId=params.getString("conversationId");
            boolean nextPage = params.getBoolean("nextPage");
            if (params.containsKey("topK")) {
                topK = params.getInteger("topK");
            }
            if (params.containsKey("imageData")) {
                imgBase64Data = params.getString("imageData");
            }
            this.chatRunningStatus.setRunning(userName, true);
            if(StringUtil.isBlank(inputPrompt)){
                inputPrompt="用户当前输入了一个图片，根据图片得到了相关商品。";
            }
            this.chatService.doChat(userName, inputPrompt, configMainId, imgBase64Data, topK, sseEmitter, nextPage,conversationId);
            sseEmitter.send("{\"data\":\"收到您的请求->后台处理中\n\"}");
        } catch (Exception e) {
            logger.error(">>>>>>ai chat error->{}", e.getMessage(), e);
            try {
                sseEmitter.send("{\"error\":\"请求出错，请稍侯\"}");
                sseEmitter.complete();
            } catch (Exception se) {
            }
        }
        return sseEmitter;
    }
}
