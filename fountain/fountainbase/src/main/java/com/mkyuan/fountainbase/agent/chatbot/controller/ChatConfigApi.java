package com.mkyuan.fountainbase.agent.chatbot.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mkyuan.fountainbase.agent.chatbot.bean.ChatConfigBackup;
import com.mkyuan.fountainbase.agent.chatbot.bean.ChatConfigDetail;
import com.mkyuan.fountainbase.agent.chatbot.bean.ChatConfigMain;
import com.mkyuan.fountainbase.agent.chatbot.service.ChatConfigService;
import com.mkyuan.fountainbase.aop.MyTokenCheck;
import com.mkyuan.fountainbase.aop.PrivilegeCheck;
import com.mkyuan.fountainbase.common.controller.response.ResponseBean;
import com.mkyuan.fountainbase.common.controller.response.ResponseCodeEnum;
import com.mkyuan.fountainbase.common.controller.response.ResponseIgnore;
import com.mkyuan.fountainbase.common.minio.RecordService;
import com.mkyuan.fountainbase.common.minio.beans.SmartArchiveRecord;
import com.mkyuan.fountainbase.common.util.SequenceUtil;
import com.mkyuan.fountainbase.knowledge.bean.UserKnowledgeMain;
import com.mkyuan.fountainbase.user.service.SystemUserMgtService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import reactor.util.function.Tuple2;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

@RestController
public class ChatConfigApi {
    protected Logger logger = LogManager.getLogger(this.getClass());

    @Autowired
    private ChatConfigService chatConfigService;

    @Autowired
    private SequenceUtil sequenceUtil;

    @Autowired
    private RecordService recordService;

    //add a chat config
    @MyTokenCheck
    @RequestMapping(value = "/api/ai/chat/saveConfig", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean saveConfig(@RequestHeader("token") String token, @RequestHeader("userName") String userName,
                                   @RequestBody JSONObject params) {
        ResponseBean responseBean = new ResponseBean();
        try {
            String configDescription = "";
            String systemMsg = "";
            String groovyRules = "";
            double temperature = 0.1;
            List<String> knowledgeRepoIdList = new ArrayList<>();
            /**
             *   前端这样传进来
             *   const allowUser = {
             *     key1: "value1",
             *     key2: "value2",
             *     key3: "value3"
             *   }
             */
            List<String> allowUsers = new ArrayList<>();
            String description = "";

            long configMainId = params.getLong("configMainId");
            if (configMainId <= 0) {
                return new ResponseBean(ResponseCodeEnum.SUCCESS.getCode(), "mainId error", -1);
            }
            description = params.getString("description");
            systemMsg = params.getString("systemMsg");
            groovyRules = params.getString("groovyRules");
            temperature = params.getDoubleValue("temperature");

            knowledgeRepoIdList = params.getJSONArray("knowledgeRepoIdList").toJavaList(String.class);
            allowUsers = params.getJSONArray("allowUsers").toJavaList(String.class);
            String rewriteSelectedDifySequenceNo = params.getString("rewriteSelectedDifySequenceNo");
            String chatSelectedDifySequenceNo = params.getString("chatSelectedDifySequenceNo");

            //组装ChatConfigMain
            ChatConfigMain chatConfigMain = new ChatConfigMain();
            chatConfigMain.setId(configMainId);
            chatConfigMain.setAllowUsers(allowUsers);
            chatConfigMain.setKnowledgeRepoIdList(knowledgeRepoIdList);
            chatConfigMain.setDescription(description);
            chatConfigMain.setSystemMsg(systemMsg);
            chatConfigMain.setCreatedBy(userName);
            chatConfigMain.setUserName(userName);
            chatConfigMain.setTemperature(temperature);
            chatConfigMain.setGroovyRules(groovyRules);
            chatConfigMain.setRewriteSelectedDifySequenceNo(rewriteSelectedDifySequenceNo);
            chatConfigMain.setChatSelectedDifySequenceNo(chatSelectedDifySequenceNo);
            //保存数据
            this.chatConfigService.addChatConfig(userName, chatConfigMain);
            return new ResponseBean(ResponseCodeEnum.SUCCESS.getCode(), "success", 1);
        } catch (Exception e) {
            logger.error(">>>>>>save config api error->{}", e.getMessage(), e);
            return new ResponseBean(ResponseCodeEnum.FAIL);
        }
    }

    //update a chat config
    @MyTokenCheck
    @RequestMapping(value = "/api/ai/chat/deleteConfig", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean deleteConfig(@RequestHeader("token") String token, @RequestHeader("userName") String userName,
                                     @RequestBody JSONObject params) {
        ResponseBean responseBean = new ResponseBean();
        long configMainId = 0l;
        try {
            if (!params.containsKey("configMainId")) {
                return new ResponseBean(ResponseCodeEnum.ILLEGAL_PARAMETERS);
            }
            configMainId = params.getLong("configMainId");
            this.chatConfigService.deleteChatConfig(userName, configMainId);
        } catch (Exception e) {
            logger.error(">>>>>>delete config error->{}", e.getMessage(), e);
        }
        return new ResponseBean(ResponseCodeEnum.SUCCESS);
    }

    //update a chat config
    @MyTokenCheck
    @RequestMapping(value = "/api/ai/chat/updateConfig", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean updateConfig(@RequestHeader("token") String token, @RequestHeader("userName") String userName,
                                     @RequestBody JSONObject params) {
        ResponseBean responseBean = new ResponseBean();
        try {
            String configDescription = "";
            String systemMsg = "";
            String groovyRules = "";

            double temperature = 0.1;
            List<String> knowledgeRepoIdList = new ArrayList<>();
            List<String> allowUsers = new ArrayList<>();
            String description = "";

            long configMainId = params.getLong("configMainId");
            if (configMainId <= 0) {
                return new ResponseBean(ResponseCodeEnum.SUCCESS.getCode(), "mainId error", -1);
            }
            description = params.getString("description");
            systemMsg = params.getString("systemMsg");
            groovyRules = params.getString("groovyRules");
            temperature = params.getDoubleValue("temperature");

            knowledgeRepoIdList = params.getJSONArray("knowledgeRepoIdList").toJavaList(String.class);
            allowUsers = params.getJSONArray("allowUsers").toJavaList(String.class);
            String rewriteSelectedDifySequenceNo = params.getString("rewriteSelectedDifySequenceNo");
            String chatSelectedDifySequenceNo = params.getString("chatSelectedDifySequenceNo");
            //组装ChatConfigMain
            ChatConfigMain chatConfigMain = new ChatConfigMain();
            chatConfigMain.setId(configMainId);
            chatConfigMain.setAllowUsers(allowUsers);
            chatConfigMain.setKnowledgeRepoIdList(knowledgeRepoIdList);
            chatConfigMain.setDescription(description);
            chatConfigMain.setSystemMsg(systemMsg);
            chatConfigMain.setCreatedBy(userName);
            chatConfigMain.setTemperature(temperature);
            chatConfigMain.setUserName(userName);
            chatConfigMain.setGroovyRules(groovyRules);
            chatConfigMain.setRewriteSelectedDifySequenceNo(rewriteSelectedDifySequenceNo);
            chatConfigMain.setChatSelectedDifySequenceNo(chatSelectedDifySequenceNo);

            //保存数据
            this.chatConfigService.updateChatConfig(userName, chatConfigMain);
            return new ResponseBean(ResponseCodeEnum.SUCCESS.getCode(), "success", 1);
        } catch (Exception e) {
            logger.error(">>>>>>update config api error->{}", e.getMessage(), e);
            return new ResponseBean(ResponseCodeEnum.FAIL);
        }
    }


    //get a chatconfig item for webside use
    @MyTokenCheck
    @RequestMapping(value = "/api/ai/chat/getChatConfigByMainId", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean getChatConfigByMainId(@RequestHeader("token") String token,
                                              @RequestHeader("userName") String userName,
                                              @RequestBody JSONObject params) {
        long configMainId = 0l;
        try {
            if (!params.containsKey("configMainId")) {
                return new ResponseBean(ResponseCodeEnum.ILLEGAL_PARAMETERS);
            }
            configMainId = params.getLong("configMainId");
            JSONObject result = this.chatConfigService.getChatConfigByMainId(userName, configMainId);
            if (result == null) {
                logger.error(">>>>>>system can not find ChatConfig for id->{}", configMainId);
                return new ResponseBean(ResponseCodeEnum.FAIL);
            }
            return new ResponseBean(ResponseCodeEnum.SUCCESS, result);
        } catch (Exception e) {
            logger.error(">>>>>>getChatConfigByMainId api error->{}", e.getMessage(), e);
            return new ResponseBean(ResponseCodeEnum.FAIL);
        }
    }

    //init chat config
    @MyTokenCheck
    @RequestMapping(value = "/api/ai/chat/initConfig", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean initConfig(@RequestHeader("token") String token, @RequestHeader("userName") String userName,
                                   @RequestBody JSONObject params) {
        ResponseBean responseBean = new ResponseBean();
        try {
            JSONObject initResult = this.chatConfigService.initChatConfig();
            responseBean = new ResponseBean(ResponseCodeEnum.SUCCESS, initResult);
        } catch (Exception e) {
            logger.error(">>>>>>initConfig api error->{}", e.getMessage(), e);
        }
        return responseBean;
    }

    //get chat config list
    @MyTokenCheck
    @RequestMapping(value = "/api/ai/chat/getChatConfigListByUserName", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean getChatConfigListByUserName(@RequestHeader("token") String token,
                                                    @RequestHeader("userName") String userName,
                                                    @RequestBody JSONObject params) {
        ResponseBean responseBean = new ResponseBean();
        try {
            List<ChatConfigMain> configList = this.chatConfigService.getChatConfigListByUserName(userName);
            responseBean = new ResponseBean(ResponseCodeEnum.SUCCESS, configList);
        } catch (Exception e) {
            logger.error(">>>>>>getChatConfigListByUserName api error->{}", e.getMessage(), e);
        }
        return responseBean;
    }

    //get chat config list
    @MyTokenCheck
    @RequestMapping(value = "/api/ai/chat/getChatConfigMainId", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean getChatConfigMainId(@RequestHeader("token") String token,
                                            @RequestHeader("userName") String userName,
                                            @RequestBody JSONObject params) {
        ResponseBean responseBean = new ResponseBean();
        try {
            //long value= RandomUtil.getRandomLong();
            long value = sequenceUtil.nextId("userChatConfig");
            responseBean = new ResponseBean(ResponseCodeEnum.SUCCESS, value);
        } catch (Exception e) {
            logger.error(">>>>>>getChatConfigMainId api error->{}", e.getMessage(), e);
            return new ResponseBean(ResponseCodeEnum.FAIL);
        }
        return responseBean;
    }

    @MyTokenCheck
    @RequestMapping(value = "/api/ai/chat/getAllKnowledgeRepo", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean getAllKnowledgeRepo(@RequestHeader("token") String token,
                                            @RequestHeader("userName") String userName,
                                            @RequestBody JSONObject params) {
        ResponseBean responseBean = new ResponseBean();
        List<UserKnowledgeMain> knowledgeRepoList = new ArrayList<>();
        try {
            knowledgeRepoList = this.chatConfigService.getAllKnowledgeListByUserName(userName);
            responseBean = new ResponseBean(ResponseCodeEnum.SUCCESS, knowledgeRepoList);
        } catch (Exception e) {
            logger.error(">>>>>>getChatConfigMainId api error->{}", e.getMessage(), e);
            responseBean = new ResponseBean(ResponseCodeEnum.SUCCESS, new ArrayList<>());
        }
        return responseBean;
    }

    @MyTokenCheck
    @RequestMapping(value = "/api/ai/chat/backupAllConfig", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean backupAllConfig(@RequestHeader("token") String token,
                                        @RequestHeader("userName") String userName, @RequestBody JSONObject params) {
        ResponseBean responseBean = new ResponseBean();
        try {
            String backupDescr = "";
            if (!params.containsKey("backupDescr")) {
                return new ResponseBean(ResponseCodeEnum.ILLEGAL_PARAMETERS);
            }
            backupDescr = params.getString("backupDescr");
            int result = this.chatConfigService.backupChatConfigByUserName(userName, backupDescr);
            return new ResponseBean(ResponseCodeEnum.SUCCESS, result);
        } catch (Exception e) {
            logger.error(">>>>>>backupAllConfig for userName->{} api error->{}", userName, e.getMessage(), e);
            responseBean = new ResponseBean(ResponseCodeEnum.SUCCESS, -1);
        }
        return responseBean;
    }

    @MyTokenCheck
    @RequestMapping(value = "/api/ai/chat/getBackupList", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean getBackupList(@RequestHeader("token") String token,
                                      @RequestHeader("userName") String userName, @RequestBody JSONObject params) {
        Pageable pageable = PageRequest.of(0, 10); // 创建一个分页请求
        Page<ChatConfigBackup> backupList = new PageImpl<>(Collections.emptyList(), pageable, 10);
        try {
            String backupDescr = "";
            String queryDate = "";
            int pageNumber = 1;
            int pageSize = 10;
            if (params.containsKey("backupDescr")) {
                backupDescr = params.getString("backupDescr");
            }
            if (params.containsKey("queryDate")) {
                queryDate = params.getString("queryDate");
            }


            if (!params.containsKey("pageNumber")) {
                pageNumber = 1;
            }
            if (!params.containsKey("pageSize")) {
                pageSize = SystemUserMgtService.PAGESIZE;
            }
            backupList = this.chatConfigService.getBackupList(userName, backupDescr, queryDate, pageNumber, pageSize);
        } catch (Exception e) {
            logger.error(">>>>>>getBackupList api error->{}", e.getMessage(), e);
        }
        return new ResponseBean(ResponseCodeEnum.SUCCESS, backupList);
    }

    @MyTokenCheck
    @RequestMapping(value = "/api/ai/chat/downloadBackupFile", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    @ResponseIgnore
    public void downloadBackupFile(@RequestHeader("token") String token,
                                   @RequestHeader("userName") String userName, @RequestBody JSONObject params,
                                   HttpServletResponse response) {

        try (OutputStream os = response.getOutputStream()) {
            String fileCode = "";
            if (!params.containsKey("fileCode")) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "no any files found");
            }
            fileCode = params.getString("fileCode");
            Tuple2<SmartArchiveRecord, byte[]> tuple = this.recordService.find(fileCode);
            SmartArchiveRecord record = tuple.getT1();
            byte[] data = tuple.getT2();
            response.setHeader("Content-Type", record.getContentType());
            response.setHeader("Content-Length", String.valueOf(data.length));
            os.write(data);
        } catch (IOException e) {
            this.logger.error(">>>>>>文件预览失败,原因:{}", e.getMessage(), e);
            try {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "resource not found");
            } catch (IOException e1) {
                this.logger.error(">>>>>>响应404错误,原因:{}", e.getMessage(), e);
            }
        } catch (Exception e) {
            logger.error(">>>>>>根据code查看上传的文件出错->{}", e.getMessage(), e);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "service error");
            } catch (IOException e1) {
                this.logger.error(">>>>>>响应500错误,原因:{}", e.getMessage(), e);
            }
        }
    }

    @MyTokenCheck
    @RequestMapping(value = "/api/ai/chat/removeBackupFile", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean removeBackupFile(@RequestHeader("token") String token,
                                         @RequestHeader("userName") String userName, @RequestBody JSONObject params) {
        try {
            String backupId = "";
            if (!params.containsKey("backupId")) {
                return new ResponseBean(ResponseCodeEnum.ILLEGAL_PARAMETERS);
            }
            backupId = params.getString("backupId");
            this.chatConfigService.removeBackupFile(userName, backupId);
        } catch (Exception e) {
            logger.error(">>>>>>removeBackupFile error->{}", e.getMessage(), e);
        }
        return new ResponseBean(ResponseCodeEnum.SUCCESS);
    }

    @MyTokenCheck
    @RequestMapping(value = "/api/ai/chat/restoreByBackupFile", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean restoreByBackupFile(@RequestHeader("token") String token,
                                            @RequestHeader("userName") String userName,
                                            @RequestBody JSONObject params) {
        try {
            String backupId = "";
            if (!params.containsKey("backupId")) {
                return new ResponseBean(ResponseCodeEnum.ILLEGAL_PARAMETERS);
            }
            backupId = params.getString("backupId");
            this.chatConfigService.restoreByBackupFile(userName, backupId);
        } catch (Exception e) {
            logger.error(">>>>>>restoreByBackupFile error->{}", e.getMessage(), e);
        }
        return new ResponseBean(ResponseCodeEnum.SUCCESS);
    }
}
