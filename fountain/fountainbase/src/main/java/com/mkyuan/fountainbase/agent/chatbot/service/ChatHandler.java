package com.mkyuan.fountainbase.agent.chatbot.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mkyuan.fountainbase.agent.chatbot.bean.ChatConfigDetail;
import com.mkyuan.fountainbase.agent.chatbot.bean.ChatConfigMain;
import com.mkyuan.fountainbase.agent.chatbot.bean.KnowledgeResult;
import com.mkyuan.fountainbase.agent.chatbot.bean.UserChatConfigBean;
import com.mkyuan.fountainbase.ai.*;
import com.mkyuan.fountainbase.ai.bean.AIModel;
import com.mkyuan.fountainbase.ai.bean.RequestPayload;
import com.mkyuan.fountainbase.ai.service.DifyService;
import com.mkyuan.fountainbase.ai.service.DifyTools;
import com.mkyuan.fountainbase.ai.service.SystemModelSupport;
import com.mkyuan.fountainbase.vectordb.bean.query.QDRantQueryResult;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import javafx.scene.paint.Stop;
import jodd.util.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ChatHandler {

    protected Logger logger = LogManager.getLogger(this.getClass());
    private ObjectMapper objectMapper = new ObjectMapper();
    public final static int USER_HISTORY_MAXSIZE = 20;
    @Autowired
    private ChatHelper chatHelper;

    @Autowired
    ChatConfigTool chatConfigTool;

    @Autowired
    private AIFactory aiFactory;

    @Autowired
    private SystemModelSupport systemModelSupport;

    @Autowired
    private ChatRunningStatus chatRunningStatus;

    @Autowired
    private AIFunctionHelper aiFunctionHelper;

    @Autowired
    private DifyTools difyTools;

    public void executeChat(AIModel aiModel, long configMainId, String userName, String inputPrompt,
                            String rewritedPrompt, List<KnowledgeResult> knowledgeList, SseEmitter emitter,
                            int chatType,String conversationId) {
        try {
            //重排序开始
            List validKnowledgeList = new ArrayList();
            //ChatConfigDetail stepDetail = new ChatConfigDetail();
            //stepDetail = chatConfigTool.getUserChatConfigDetailStep(userName, configMainId, 5);
            if (chatType == 1) {
                //需要进行重排序
                validKnowledgeList = this.chatHelper.rerank(rewritedPrompt, knowledgeList);
                //validKnowledgeList.clear();
                //validKnowledgeList.addAll(knowledgeList);
                //validKnowledgeList = this.chatHelper.rerank(inputPrompt, knowledgeList);
            } else {
                validKnowledgeList.clear();
                validKnowledgeList.addAll(knowledgeList);
            }
            if (validKnowledgeList == null || validKnowledgeList.isEmpty()) {
                validKnowledgeList = new ArrayList();
                validKnowledgeList.addAll(knowledgeList);
            }
            JSONObject sendObj = new JSONObject();
            sendObj.put("data", "检索和匹配用户回答内容: " + validKnowledgeList.size() + " 条\n");
            emitter.send(sendObj);
            UserChatConfigBean userChatConfigBean = chatConfigTool.getUserChatConfig(userName, configMainId);
            String chatDifSequenceNo = userChatConfigBean.getChatDifySequenceNo();
            //重排序结束
            /*
            UserChatConfigBean userChatConfigBean = chatConfigTool.getUserChatConfig(userName, configMainId);
            String groovyRules = userChatConfigBean.getGroovyRules();
            RequestPayload requestPayload = new RequestPayload();
            requestPayload = this.getSendMessages(userName, inputPrompt, validKnowledgeList, configMainId);
            requestPayload.setModel(aiModel.getModelName());
            requestPayload.setStream(true);
            requestPayload.setTemperature(userChatConfigBean.getGlobalTemperature());
            String requestPayloadStr = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                    objectMapper.readTree(objectMapper.writeValueAsString(requestPayload)));
            logger.info("发送请求为：\n{}", requestPayloadStr);
            AIModel masterModel = this.systemModelSupport.getMasterModel(userName);
            //StringBuilder sendPromptMsg = new StringBuilder();
            AIComponent aiComponent = this.aiFactory.getAIComponent(masterModel.getType());

             */
            List<String> thinkWords = new ArrayList<>();
            List<String> words = new ArrayList<>();
            //聊天前先把提问历史塞进对话上下文历史库
            this.chatHelper.addHistoryPrompt(userName, inputPrompt, USER_HISTORY_MAXSIZE);
            String knowledgeStr = objectMapper.writeValueAsString(validKnowledgeList);
            this.executeAIRequest(userName, inputPrompt, chatDifSequenceNo, knowledgeStr, emitter, words, thinkWords,conversationId);
        } catch (Exception e) {
            logger.error(">>>>>>发送用户SSE对话请求失败->{}", e.getMessage(), e);
            try {
                emitter.send("{\"error\":\"系统繁忙，请稍后重试\"}");
                emitter.complete();  // 重要：提前返回
                return;
            } catch (Exception ee) {
            }
        }
    }

    private void getResultFromAIAnswer(String userName, String groovyRules, String aiAnswer, String knowledgeStr,
                                       AIModel model, AIFunctionHelper aiFunctionHelper, ChatHelper chatHelper,
                                       SseEmitter emitter) {
        try {
            //执行groovy脚本
            Binding binding = new Binding();
            // 传递必要的参数给groovy脚本
            binding.setVariable("chatHelper", chatHelper);
            binding.setVariable("userName", userName);
            binding.setVariable("model", model);
            binding.setVariable("aiFunctionHelper", aiFunctionHelper);
            binding.setVariable("emitter", emitter);
            binding.setVariable("aiAnswer", aiAnswer);
            binding.setVariable("knowledgeStr", knowledgeStr);
            GroovyShell shell = new GroovyShell(binding);
            // 执行脚本并获取返回结果
            logger.info(">>>>>>execute the external groovyReuls");
            List<String> result = (List<String>) shell.evaluate(groovyRules);
            logger.info(">>>>>>execute external groovyRules result->{}", result.size());
            if (result != null && !result.isEmpty()) {
                Map<String, Object> sendObj = new HashMap<>();
                sendObj.put("dataIds", result);
                emitter.send(sendObj);
            }
            for (String s : result) {
                logger.info(">>>>>>groovyRules execute result->{}", s);
            }
        } catch (Exception e) {
            logger.error(">>>>>>execute processDataHandle groovyRules error->{}", e.getMessage(), e);
        }
    }

    private void executeAIRequest(String userName, String sendPrompt, String chatDifySequenceNo, String knowledgeStr,
                                  SseEmitter emitter,
                                  List<String> words, List<String> thinkWords,String conversationId) {
        // 添加标记来追踪是否在产品ID收集模式
        try {
            Map<String, String> stringrizedparas = new HashMap<>();
            stringrizedparas.put("productList", knowledgeStr);
            this.difyTools.invokeWithStream(userName, conversationId, sendPrompt, chatDifySequenceNo, stringrizedparas,
                                            new CompleteCallback() {
                                                @Override
                                                public void onDone() {
                                                    try {
                                                        String answerLine = String.join("", words);
                                                        chatHelper.addHistoryAnswer(userName, answerLine,
                                                                                    USER_HISTORY_MAXSIZE);
                                                        emitter.send("{\"done\":true}");
                                                    } catch (Exception e) {
                                                        logger.error(">>>>>>exception in onDone ->{}", e.getMessage(),
                                                                     e);
                                                    } finally {
                                                        chatRunningStatus.setRunning(userName, false);
                                                        try {
                                                            emitter.complete();
                                                        } catch (Exception e) {
                                                            // IGNORE
                                                        }
                                                    }
                                                }

                                                @Override
                                                public void onData(CompleteResponseChunk chunk) {
                                                    try {
                                                        String content = "";
                                                        String think = "";
                                                        if (chunk.getThink() != null) {
                                                            think = chunk.getThink();
                                                        }
                                                        if (chunk.getContent() != null) {
                                                            content = chunk.getContent();
                                                        }

                                                        Map<String, Object> sendObj = new HashMap<>();
                                                        sendObj.put("data", content);
                                                        sendObj.put("think", think);
                                                        sendObj.put("conversationId", chunk.getConversationId());
                                                        sendObj.put("dataIds", chunk.getProductIds());
                                                        emitter.send(sendObj);
                                                        words.add(content);
                                                        thinkWords.add(think);
                                                    } catch (Exception e) {
                                                    }
                                                }

                                                @Override
                                                public void onError(Exception e) {
                                                    logger.error(">>>>>>收到AI端返回错误->{}", e.getMessage());

                                                }
                                            });
        } catch (Exception e) {
            logger.error(">>>>>>onError内部处理错误->{}", e.getMessage(), e);
            chatRunningStatus.setRunning(userName, false);
            return;
        }
    }

    private RequestPayload getSendMessages(String userName, String inputPrompt, List<KnowledgeResult> knowledgeList,
                                           long configMainId) {
        RequestPayload messagePayload = new RequestPayload();
        String systemMessageStr = "";
        try {
            UserChatConfigBean userChatConfigBean = chatConfigTool.getUserChatConfig(userName, configMainId);
            if (userChatConfigBean != null) {
                systemMessageStr = userChatConfigBean.getSystemMsg();
            }
            //把用户的inputPrompt+knowledgeResult
            JSONArray knowledgeBaseArray = this.getKnowledgeBaseStr(knowledgeList);
            JSONObject sendPromptObj = new JSONObject();
            sendPromptObj.put("用户当前提问", inputPrompt);
            if (knowledgeList != null && !knowledgeList.isEmpty()) {
                sendPromptObj.put("knowledgeBase", knowledgeBaseArray);
            } else {
                sendPromptObj.put("knowledgeBase", new ArrayList<>());
            }
            String sendPromptStr = objectMapper.writeValueAsString(sendPromptObj);
            messagePayload = chatHelper.generateUserSendMsg(userName, systemMessageStr, sendPromptStr);
        } catch (Exception e) {
            logger.error(">>>>>>getSendMessages error->{}", e.getMessage(), e);
        }
        return messagePayload;
    }

    private JSONArray getKnowledgeBaseStr(List<KnowledgeResult> knowledgeList) {
        //StringBuilder knowledgeStr=new StringBuilder();
        JSONArray knowledgeArray = new JSONArray();
        try {
            if (knowledgeList != null && !knowledgeList.isEmpty()) {
                for (KnowledgeResult knowledgeResult : knowledgeList) {
                    JSONObject knowledgeItem = new JSONObject();
                    knowledgeItem.put("文件名", knowledgeResult.getFileName());
                    knowledgeItem.put("知识库内容", knowledgeResult.getFileContent());
                    knowledgeArray.add(knowledgeItem);
                }
            }
            return knowledgeArray;
        } catch (Exception e) {
            logger.error(">>>>>>getKnowledgeBaseStr error->{}", e.getMessage(), e);
        }
        return new JSONArray();
    }
}
