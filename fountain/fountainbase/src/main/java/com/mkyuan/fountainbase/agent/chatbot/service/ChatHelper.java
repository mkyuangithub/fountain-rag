package com.mkyuan.fountainbase.agent.chatbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mkyuan.fountainbase.agent.chatbot.bean.ChatConfigDetail;
import com.mkyuan.fountainbase.agent.chatbot.bean.KnowledgeResult;
import com.mkyuan.fountainbase.agent.chatbot.bean.UserChatConfigBean;
import com.mkyuan.fountainbase.ai.AIComponent;
import com.mkyuan.fountainbase.ai.AIFactory;
import com.mkyuan.fountainbase.ai.AIFunctionHelper;
import com.mkyuan.fountainbase.ai.bean.AIModel;
import com.mkyuan.fountainbase.ai.bean.Message;
import com.mkyuan.fountainbase.ai.bean.RequestPayload;
import com.mkyuan.fountainbase.ai.service.SystemModelSupport;
import com.mkyuan.fountainbase.common.util.okhttp.OkHttpHelper;
import jodd.util.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

@Component
public class ChatHelper {

    protected Logger logger = LogManager.getLogger(this.getClass());
    private ObjectMapper objectMapper = new ObjectMapper();
    private final static String HistoryInputRedisKey = "fountain:chat:historychat:prompt:";//后面要加userName
    private final static String HistoryAnswerRedisKey = "fountain:chat:historychat:answer:";//后面要加userName
    private final static String KNOWLEDGE_FOR_PAGED_KEY="fountain:chat:knowledgeList:paged:";//后面要加userName
    private final static String REWRITE_PROMPT="fountain:chat:rewritedprompt:";//后面跟userName
    public final static int USER_HISTORY_MAXSIZE=20;
    @Autowired
    private ChatConfigTool chatConfigTool;

    @Autowired
    private AIFunctionHelper aiFunctionHelper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${fountain.ai.rerank.rerankUrl}")
    private String rerankUrl = "";

    @Value("${fountain.ai.rerank.rerankApiKey}")
    private String rerankApiKey = "";

    @Autowired
    private OkHttpHelper okHttpHelper;

    @Autowired
    private AIFactory aiFactory;

    @Autowired
    private SystemModelSupport systemModelSupport;

    //存入每次会话时的knowledgeList，覆盖式
    public void storeKnowledgeListToMem(String userName, String rewritedPrompt, List<KnowledgeResult> knowledgeList) {
        String rewritedPromptRedisKey=REWRITE_PROMPT+userName;
        String redisKey=KNOWLEDGE_FOR_PAGED_KEY+userName;
        if (knowledgeList == null || knowledgeList.isEmpty()) {
            return;
        }
        // 按照score降序排序
        knowledgeList.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));

        // 先删除旧的数据
        redisTemplate.delete(redisKey);

        // 使用rightPushAll将所有元素添加到列表末尾
        redisTemplate.opsForList().rightPushAll(redisKey, knowledgeList);
        redisTemplate.opsForValue().set(rewritedPromptRedisKey,rewritedPrompt);
    }
    public boolean hasMoreData(String userName){
        boolean result=false;
        try{
            String redisKey=KNOWLEDGE_FOR_PAGED_KEY+userName;

            if(!redisTemplate.hasKey(redisKey)){
                result=false;
                return result;
            }
            // 使用List操作而不是Hash操作
            Long size = redisTemplate.opsForList().size(redisKey);
            return size != null && size > 0;
        }catch(Exception e){
            logger.error(">>>>>>get hasMoreData error->{}",e.getMessage(),e);
        }
        return result;
    }
    public void cleanPagedKnowledgeList(String userName){
        String rewritedPromptRedisKey=REWRITE_PROMPT+userName;
        String pagedKnowledgeListRedisKey = KNOWLEDGE_FOR_PAGED_KEY + userName;
       redisTemplate.delete(rewritedPromptRedisKey);
       redisTemplate.delete(pagedKnowledgeListRedisKey);
    }
    public String getRewritedPrompt(String userName){
        String rewritedPromptRedisKey=REWRITE_PROMPT+userName;
        Object obj=redisTemplate.opsForValue().get(rewritedPromptRedisKey);
        if(obj!=null){
            String result=(String)obj;
            return result;
        }
        return "";
    }
    //每次获取时分页取出
    /**
     * 从Redis中获取并移除指定数量的知识条目
     * @param userName 用户唯一标识
     * @param nums 需要获取的条目数
     *      *
     * @return 知识列表
     */
    public List<KnowledgeResult> popKnowledgeList(String userName, int nums) {
        String redisKey = KNOWLEDGE_FOR_PAGED_KEY + userName;
        if (nums <= 0) {
            return new ArrayList<>();
        }

        // 先检查列表是否存在及其长度
        Long size = redisTemplate.opsForList().size(redisKey);
        if (size == null || size == 0) {
            return new ArrayList<>();
        }

        // 获取实际可以pop的数量（取较小值）
        int actualNums = Math.min(nums, size.intValue());

        List<KnowledgeResult> result = new ArrayList<>();
        // 循环获取指定数量的元素
        for (int i = 0; i < actualNums; i++) {
            KnowledgeResult knowledge = (KnowledgeResult)redisTemplate.opsForList().leftPop(redisKey);
            if (knowledge == null) {
                break;
            }
            //logger.info(">>>>>>leftPop取出数据->score: {} 文件名: {}",knowledge.getScore(),knowledge.getFileName());
            result.add(knowledge);
        }

        // 如果已经是最后的数据了，可以直接删除这个key
        if (redisTemplate.opsForList().size(redisKey) == 0) {
            redisTemplate.delete(redisKey);
        }
        return result;
    }
    public boolean allowChat(String userId,String userName,long configMainId){
        boolean allow=false;
        UserChatConfigBean userChatConfigBean=chatConfigTool.getUserChatConfig(userName,configMainId);
        if(userChatConfigBean==null){
            logger.error(">>>>>>取用户配置错误");
            return false;
        }
        if(userChatConfigBean.getAllowUsers().contains(userId)){
            return true;
        }
        return allow;
    }
    public String reWritePrompt(String userName, long configMainId,String originalPrompt){
        StringBuilder rewritedPrompt=new StringBuilder();
        try {
            rewritedPrompt.append(this.aiFunctionHelper.getFunctions(20001, originalPrompt));
            StringBuilder requirement = new StringBuilder();
            //串联流程123
            for (int type = 1; type <= 3; type++) {
                ChatConfigDetail stepDetail = chatConfigTool.getUserChatConfigDetailStep(userName, configMainId, type);
                if (stepDetail.isEnabled()) {
                    requirement.append(stepDetail.getStepPrompt()).append("\n");
                }
            }
            String requirementTxt = requirement.toString();
            String finalPrompt = rewritedPrompt.toString().replace("$<requirement>", requirementTxt);
            List<String> histPromptList = this.getHistoryPrompt(userName, USER_HISTORY_MAXSIZE);

            finalPrompt = finalPrompt.replace("$<historyPromptList>", objectMapper.writeValueAsString(histPromptList));
            return finalPrompt;
        }catch(Exception e){
            logger.error(">>>>>>build final rewrite prompt requirement error->{}",e.getMessage(),e);
            return originalPrompt;
        }
    }

    public void newSession(String userName) {
        try {
            String historyPromptKey = HistoryInputRedisKey + userName;
            String historyAnswerKey = HistoryAnswerRedisKey + userName;
            if (redisTemplate.hasKey(historyPromptKey)) {
                redisTemplate.delete(historyPromptKey);
            }
            if (redisTemplate.hasKey(historyAnswerKey)) {
                redisTemplate.delete(historyAnswerKey);
            }
            this.cleanPagedKnowledgeList(userName);
        } catch (Exception e) {
            logger.error(">>>>>>新起一个chatbot会话出错->{}", e.getMessage(), e);
        }
    }

    public void addHistoryPrompt(String userName, String input, int maxSize) {
        List<String> promptHists = new ArrayList<>();
        String promptRedisKey = HistoryInputRedisKey +userName;
        try {
            if(StringUtil.isBlank(input)){
                return;
            }
            Object obj = this.redisTemplate.opsForValue().get(promptRedisKey);
            if (obj != null) {
                promptHists = (List<String>) obj;
            }
            //logger.info(">>>>>>current promptHists size->{}",promptHists.size());
            int size = promptHists.size();
            if (size > 0 && size >= maxSize) {
                promptHists.remove(0);
            }
            promptHists.add(input);
            redisTemplate.opsForValue().set(promptRedisKey, promptHists);
        } catch (Exception e) {
            logger.error(">>>>>>往Redis里插入用户历史的提示语出错->{}", e.getMessage(), e);
        }
    }
    public List<String> getHistoryPrompt(String userName, int maxSize) {
        List<String> promptHists = new ArrayList<>();
        String promptRedisKey = HistoryInputRedisKey +userName;
        try {

            Object obj = this.redisTemplate.opsForValue().get(promptRedisKey);
            if (obj != null) {
                promptHists = (List<String>) obj;
            }
           return promptHists;
        } catch (Exception e) {
            logger.error(">>>>>>往Redis里查询用户历史的提示语出错->{}", e.getMessage(), e);
        }
        return promptHists;
    }
    public void addHistoryAnswer(String userName, String answer, int maxSize) {
        String cleanedAnswer = answer.replaceAll("(?s)<think>.*?</think>", "");

        List<String> answerHists = new ArrayList<>();
        String answerHistRedisKey = HistoryAnswerRedisKey + userName;
        try {
            if(StringUtil.isBlank(answer)){
                return;
            }
            Object obj = this.redisTemplate.opsForValue().get(answerHistRedisKey);
            if (obj != null) {
                answerHists = (List<String>) obj;
            }
            int size = answerHists.size();
            if (size > 0 && size >= maxSize) {
                answerHists.remove(0);  // 删除最旧的记录(列表的第一个元素)
            }
            answerHists.add(cleanedAnswer);  // 添加新记录到列表末尾
            redisTemplate.opsForValue().set(answerHistRedisKey, answerHists);
        } catch (Exception e) {
            logger.error(">>>>>>往Redis里插入AI历史的回答出错->{}", e.getMessage(), e);
        }
    }

    private List<Object> getHistInput(String userName) {
        List<Object> histInputMessages = new ArrayList<>();
        String promptRedisKey = HistoryInputRedisKey + userName;
        try {
            List<String> histPrompt = new ArrayList<>();
            Object obj = redisTemplate.opsForValue().get(promptRedisKey);
            if (obj != null) {
                histPrompt = (List<String>) obj;
            }
            for (String userPrompt : histPrompt) {
                Message userMessasge=new Message();
                userMessasge.setRole("user");
                userMessasge.setContent(userPrompt);
                histInputMessages.add(userMessasge);
            }
        } catch (Exception e) {
            logger.error(">>>>>>从Redis里取用户历史的提示语出错->{}", e.getMessage(), e);
        }
        return histInputMessages;
    }

    private List<Object> getHistAnswer(String userName) {
        List<Object> histAnswerMessages = new ArrayList<>();
        String answerHistRedisKey = HistoryAnswerRedisKey + userName;
        try {
            List<String> histAnswer = new ArrayList<>();
            Object obj = redisTemplate.opsForValue().get(answerHistRedisKey);
            if (obj != null) {
                histAnswer = (List<String>) obj;
            }
            for (String answerStr : histAnswer) {
                Message answerMessage=new Message();
                answerMessage.setRole("assistant");
                answerMessage.setContent(answerStr);
                histAnswerMessages.add(answerMessage);
            }
        } catch (Exception e) {
            logger.error(">>>>>>从Redis里取AI历史的回答出错->{}", e.getMessage(), e);
        }
        return histAnswerMessages;
    }
    public RequestPayload generateUserSendMsg(String userName, String systemMsgStr, String userInputPrompts) {
        RequestPayload requestPayload = new RequestPayload();
        try {
            //必须先组装system角色内容
            Message systemMessage = new Message();
            systemMessage.setRole("system");
            systemMessage.setContent(systemMsgStr);
            requestPayload.getMessages().add(systemMessage);
            //创建历史提示语
            List<Object> histPromptContent = this.getHistInput(userName);
            List<Object> histAnswerList = this.getHistAnswer(userName);

            if (histPromptContent != null && !histPromptContent.isEmpty()) {
                for (int i = 0; i < histPromptContent.size(); i++) {
                    Message promptContent = i < histPromptContent.size() ?
                            (Message)histPromptContent.get(i) : new Message();
                    Message answerContent = i < histAnswerList.size() ?
                            (Message)histAnswerList.get(i) : new Message();

                    Message histPromptMessage = new Message();
                    try {
                        String promptHistMsg = "";
                        if (StringUtil.isNotBlank(promptContent.getContent())) {
                            promptHistMsg = promptContent.getContent();
                        }
                        histPromptMessage.setRole("user");
                        histPromptMessage.setContent(promptHistMsg);
                        requestPayload.getMessages().add(histPromptMessage);
                    } catch (Exception e) {

                    }
                    try {
                        String answerHistMsg = "";
                        if (StringUtil.isNotBlank(answerContent.getContent())) {
                            answerHistMsg = answerContent.getContent();
                        }
                        Message histAnswerMessage = new Message();
                        histAnswerMessage.setRole("assistant");
                        histAnswerMessage.setContent(answerContent.getContent());
                        requestPayload.getMessages().add(histAnswerMessage);
                    } catch (Exception e) {
                    }
                }
            }

            //创建用户当前提示语

            Message userMessage = new Message();
            userMessage.setRole("user");
            // 创建UserContent
            userMessage.setContent(userInputPrompts);
            requestPayload.getMessages().add(userMessage);


        } catch (Exception e) {
            logger.error(">>>>>>产生历史会话消息的payload出错->{}", e.getMessage(), e);
        }

        return requestPayload;
    }

    public List<KnowledgeResult> rerank(String rewritePrompt, List<KnowledgeResult> knowledgeList){
        List<KnowledgeResult> result=new ArrayList<>();
        try{
            Map<String,String> headers=new HashMap<>();
            headers.put("Content-Type","application/json");
            headers.put("apiKey",rerankApiKey);
            Map<String,Object> bodyPayload=new HashMap<>();
            bodyPayload.put("userInput",rewritePrompt);
            bodyPayload.put("knowledgeList",knowledgeList);
            String jsonResponse=this.okHttpHelper.postJsonWithMultiHeaders(rerankUrl,bodyPayload,headers);
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);
            String prettyJson = objectMapper.writerWithDefaultPrettyPrinter()
                                            .writeValueAsString(jsonNode);
            logger.info(">>>>>>Rerank Response JSON:\n{}", prettyJson);

            JSONObject rootObject = new JSONObject(jsonResponse);
            JSONObject data = rootObject.getJSONObject("data");
            JSONArray results = data.getJSONArray("resultList");
            for (int i = 0; i < results.length(); i++) {
                JSONObject item = results.getJSONObject(i);
                KnowledgeResult knowledge = new KnowledgeResult();
                knowledge.setPointId(item.optLong("pointId", 0L));
                knowledge.setFileName(item.optString("fileName", ""));
                knowledge.setFileContent(item.optString("fileContent", ""));
                knowledge.setScore(Float.parseFloat(String.valueOf(item.optDouble("score", 0.0))));
                result.add(knowledge);
            }
            return result;
        }catch(Exception e){
            result=new ArrayList<>();
            result.addAll(knowledgeList);
            logger.error(">>>>>>rerank error->{}",e.getMessage(),e);
            return result;
        }
    }
    public String safeJsonCall(String userName, String prompt, AIModel aiModel, double temperature) {
        int pc = 1;
        String jsonResponse = "";
        try {
            AIComponent aiComponent = this.aiFactory.getAIComponent(aiModel.getType());
            RequestPayload requestPayload = new RequestPayload();
            requestPayload.setTemperature(temperature);
            requestPayload.setStream(false);
            requestPayload.setModel(aiModel.getModelName());
            List<Object> messages = new ArrayList<>();
            Message userMessage = new Message();
            userMessage.setRole("user");
            userMessage.setContent(prompt.toString());
            messages.add(userMessage);
            requestPayload.setMessages(messages);
            jsonResponse = aiComponent.jsonCall(aiModel, requestPayload);
            if (this.isValidJson(jsonResponse)) {
                return jsonResponse;
            } else {
                throw new Exception(">>>>>>master call jsonResponse->" + jsonResponse + " is not a valid response");
            }
        } catch (Exception e) {
            try {
                logger.error(">>>>>>use master json call ai error->{}, switch to slave json call", e.getMessage(), e);
                if (pc >= 2) {
                    logger.error(">>>>>>use slave model call has error->{}", e.getMessage(), e);
                    return "";
                }
                AIModel slaveModel = this.systemModelSupport.getSlave(userName);
                jsonResponse = this.safeJsonCall(userName, prompt, slaveModel, temperature);
                if (this.isValidJson(jsonResponse)) {
                    return jsonResponse;
                } else {
                    throw new Exception(">>>>>>slave call jsonResponse->" + jsonResponse + " is not a valid response");
                }
            } catch (Exception ae) {
                logger.error(">>>>>>safeJsonCall error->{}", e.getMessage());
                return "";
            }
        }
    }

    private boolean isValidJson(String jsonString) {
        try {
            if (jsonString.trim().startsWith("{")) {
                // 尝试解析为JSONObject
                new JSONObject(jsonString);
            } else if (jsonString.trim().startsWith("[")) {
                // 尝试解析为JSONArray
                new org.json.JSONArray(jsonString);
            } else {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
