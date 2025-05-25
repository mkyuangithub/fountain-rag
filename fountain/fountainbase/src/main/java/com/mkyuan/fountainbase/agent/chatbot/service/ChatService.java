package com.mkyuan.fountainbase.agent.chatbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mkyuan.fountainbase.agent.chatbot.bean.*;
import com.mkyuan.fountainbase.ai.AIComponent;
import com.mkyuan.fountainbase.ai.AIFactory;
import com.mkyuan.fountainbase.ai.AIFunctionHelper;
import com.mkyuan.fountainbase.ai.bean.AIModel;
import com.mkyuan.fountainbase.ai.bean.Message;
import com.mkyuan.fountainbase.ai.bean.RequestPayload;
import com.mkyuan.fountainbase.ai.service.DifyTools;
import com.mkyuan.fountainbase.ai.service.SystemModelSupport;
import com.mkyuan.fountainbase.common.util.es.ESHelper;
import com.mkyuan.fountainbase.common.util.okhttp.OkHttpHelper;
import com.mkyuan.fountainbase.knowledge.bean.UserKnowledgeMain;

import com.mkyuan.fountainbase.knowledge.service.KnowledgeVectorHelper;
import com.mkyuan.fountainbase.vectordb.VectorDbRepo;
import com.mkyuan.fountainbase.vectordb.VectorDbRepoFactory;
import com.mkyuan.fountainbase.vectordb.bean.query.*;
import jodd.util.StringUtil;
import org.apache.commons.codec.language.DaitchMokotoffSoundex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class ChatService {
    protected Logger logger = LogManager.getLogger(this.getClass());
    private ObjectMapper objectMapper = new ObjectMapper();
    public final static int DEFAULT_TOPK = 10;
    public final static int SEARCHED_RESULT = 30;
    @Autowired
    private VectorDbRepoFactory vectorDbRepoFactory;

    @Autowired
    private AIFactory aiFactory;

    @Autowired
    private SystemModelSupport systemModelSupport;

    @Autowired
    private AIFunctionHelper aiFunctionHelper;

    @Autowired
    private ChatHelper chatHelper;

    @Autowired
    private KnowledgeVectorHelper knowledgeVectorHelper;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ChatConfigTool chatConfigTool;

    @Autowired
    private ChatHandler chatHandler;

    @Autowired
    private OkHttpHelper okHttpHelper;

    @Autowired
    private ESHelper esHelper;

    @Autowired
    private DifyTools difyTools;


    public void cleanLocalSession(String userName) {
        this.chatHelper.newSession(userName);
    }

    public void cleanSession(String userName, long configMainId) {
        UserChatConfigBean userChatConfigBean = new UserChatConfigBean();
        userChatConfigBean = chatConfigTool.getUserChatConfig(userName, configMainId);
        if (userChatConfigBean != null) {
            String chatDifSequence = userChatConfigBean.getChatDifySequenceNo();
            this.difyTools.cleanAllSession(userName, chatDifSequence);
        }
    }

    public boolean allowChat(String userId, String userName, long configMainId) {
        return chatHelper.allowChat(userId, userName, configMainId);
    }

    @Async
    public void doChat(String userName, String inputPrompt, long configMainId, String imgBase64Data, int topK,
                       SseEmitter emitter,
                       boolean nextPage,String conversationId) {
        JSONObject sseMessage = new JSONObject();
        String userInputPrompt = new String(inputPrompt);
        try {
            List chatedKnowledgeList = new ArrayList();
            AIModel masterModel = this.systemModelSupport.getMasterModel(userName);
            if (nextPage) {
                String appendRequirement = "";
                appendRequirement = this.aiFunctionHelper.getFunctions(30001, "");
                logger.info(">>>>>>AI翻页时追加的要求->{}", appendRequirement);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("追加要求", appendRequirement);
                String sendPrompt = userInputPrompt + jsonObject.toString();
                masterModel = this.systemModelSupport.getMasterModel(userName);
                List<KnowledgeResult> pagedResult = chatHelper.popKnowledgeList(userName, DEFAULT_TOPK);
                String rewritedPrompt = chatHelper.getRewritedPrompt(userName);
                this.storeRetriveResultToMongo(userName, masterModel.getModelName(), sendPrompt, rewritedPrompt,
                                               pagedResult);
                chatHandler.executeChat(masterModel, configMainId, userName, sendPrompt, rewritedPrompt, pagedResult,
                                        emitter, 1,conversationId);//对话流程进入
            } else {
                int chatType = 1;
                UserChatConfigBean userChatConfigBean = new UserChatConfigBean();
                userChatConfigBean = chatConfigTool.getUserChatConfig(userName, configMainId);
                String repoId = userChatConfigBean.getKnowledgeRepoIdList().get(0);
                double temperature = userChatConfigBean.getGlobalTemperature();
                sseMessage.put("data", "AI正在理解您的提问\n");
                emitter.send(sseMessage);
                List<QDRantQueryResult> searchVlResult = this.getVectorVLSearchResult(userName, repoId,
                                                                                      imgBase64Data,
                                                                                      3);//去VL向量库搜索
                StringBuilder vlRewriteStr = new StringBuilder();
                StringBuilder vlResultStr = new StringBuilder();
                if (null != searchVlResult && !searchVlResult.isEmpty()) {
                    for (QDRantQueryResult vlResult : searchVlResult) {
                        String vlContent = vlResult.getPayload().getContent();
                        logger.info(">>>>>>从vl库搜到记录->{}，得分->{}", vlContent,
                                    vlResult.getScore());
                        vlRewriteStr.append(",").append(vlResult.getPayload().getContent());
                        vlResultStr.append(vlContent).append("搜索得分:").append(vlResult.getScore()).append(",");
                    }
                }
                if (StringUtil.isNotBlank(imgBase64Data) && StringUtil.isNotBlank(vlResultStr)) {
                    userInputPrompt += "," + vlRewriteStr;
                }
                RewritedQueryResult rewritedQueryResult = this.llmRewrite(userName, userInputPrompt,
                                                                          userChatConfigBean);//重写用户提示语以及判断是否闲聊步骤
                chatType = rewritedQueryResult.getChatType(); //取得用户是否闲聊的标志，如果是0，代表闲聊


                //logger.info(">>>>>>searchVlResult size->{}",searchVlResult.size());


                String rewritedPrompt = rewritedQueryResult.getRewritedPrompt();//取得AI重写的Query


                sseMessage = new JSONObject();
                sseMessage.put("data", "AI正在搜索相关答案\n");
                emitter.send(sseMessage.toString(), MediaType.APPLICATION_JSON);
                //logger.info(">>>>>>After llm rewrtied query is->{}", rewrtiedPrompt);
                //先进行向量库的查询
                if (chatType == 1) { //只有不是闲聊时才会走混合检索过程
                    List<QDRantQueryResult> searchResult = this.getVectorSearchResult(userName, repoId, rewritedPrompt,
                                                                                      SEARCHED_RESULT);//去向量库搜索

                    List<KnowledgeResult> esSearchResult = new ArrayList<>();

                    //ChatConfigDetail step = this.chatConfigTool.getUserChatConfigDetailStep(userName, configMainId, 4);
                    boolean esStatus = this.esHelper.queryEsStatus();
                    logger.info(">>>>>>esStatus->{}", esStatus);
                    if (esStatus) { //需要进行多路召回
                        //进入es搜索
                        esSearchResult = this.knowledgeVectorHelper.queryFromEs(repoId, rewritedPrompt);
                        for (KnowledgeResult esSearchItem : esSearchResult) {
                            logger.info(">>>>>>es item-> fileName->{} content->{}", esSearchItem.getFileName(),
                                        esSearchItem.getFileContent());
                        }
                    }

                    List<KnowledgeResult> knowledgeList = new ArrayList<>();
                    //先把qdrant的数据组装进knowledgeList
                    for (QDRantQueryResult qdRantQueryResult : searchResult) {
                        logger.info(">>>>>>qdrant-> pointId->{} content->{} score->{}", qdRantQueryResult.getId(),
                                    qdRantQueryResult.getPayload().getContent(), qdRantQueryResult.getScore());
                        KnowledgeResult knowledgeResult = new KnowledgeResult();
                        knowledgeResult.setPointId(qdRantQueryResult.getId());
                        knowledgeResult.setFileName(qdRantQueryResult.getPayload().getFileName());
                        knowledgeResult.setFileContent(qdRantQueryResult.getPayload().getContent());
                        knowledgeResult.setScore(qdRantQueryResult.getScore());
                        knowledgeList.add(knowledgeResult);
                    }
                    //再把es搜索结果合并到knowledgeList
                    Set<Long> existingPointIds = new HashSet<>();
                    int totalEsResults = esSearchResult.size();
                    int repeatedCount = 0;
                    // 首先收集已有的pointId
                    for (KnowledgeResult kr : knowledgeList) {
                        existingPointIds.add(kr.getPointId());
                    }
                    // 合并ES搜索结果，同时统计重复记录
                    for (KnowledgeResult esResult : esSearchResult) {
                        logger.info(">>>>>>esResult-> pointId->{} content->{}", esResult.getPointId(),
                                    esResult.getFileContent());
                        if (!existingPointIds.contains(esResult.getPointId())) {
                            logger.info(">>>>>>no repeat es result->id: {} content: {}", esResult.getPointId(),
                                        esResult.getFileContent());
                            knowledgeList.add(esResult);
                        } else {
                            repeatedCount++;
                        }
                    }

                    logger.info(">>>>>>esResult total result->{}, repeated record->{}, valid record->{}",
                                totalEsResults,
                                repeatedCount,
                                totalEsResults - repeatedCount);
                    sseMessage = new JSONObject();
                    sseMessage.put("data", "当前共搜索到->" + knowledgeList.size() + "条答案，准备进入回答\n");
                    emitter.send(sseMessage.toString(), MediaType.APPLICATION_JSON);
                    //得到搜索结果后落库进行分析

                    this.storeRetriveResultToMongo(userName, masterModel.getModelName(), userInputPrompt,
                                                   rewritedPrompt,
                                                   knowledgeList);
                    this.chatHelper.storeKnowledgeListToMem(userName, rewritedPrompt, knowledgeList);//存入全部的召回
                    chatedKnowledgeList = new ArrayList();
                    chatedKnowledgeList.addAll(
                            this.chatHelper.popKnowledgeList(userName, DEFAULT_TOPK));//pop出来nums条-暂设10用来聊天
                } else {
                    String appendRequirement = "";
                    appendRequirement = this.aiFunctionHelper.getFunctions(30002, "");
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("重要补充要求", appendRequirement);
                    userInputPrompt = userInputPrompt + jsonObject.toString();
                    chatedKnowledgeList = new ArrayList();
                }
                this.chatHandler.executeChat(masterModel, configMainId, userName, userInputPrompt, rewritedPrompt,
                                             chatedKnowledgeList,
                                             emitter, chatType,conversationId);//对话流程进入d
            }
        } catch (Exception e) {
            logger.error(">>>>>>doChat error->{}", e.getMessage(), e);
            try {
                emitter.send("{\"data\":\"后台运行错误，请重试。\"}");
                emitter.complete();
            } catch (Exception ee) {
            }
        }
    }

    public boolean hasMoreData(String userName) {
        return this.chatHelper.hasMoreData(userName);
    }


    private List<QDRantQueryResult> getVectorSearchResult(String userName, String repoId, String inputPrompt,
                                                          int topK) {
        logger.info(">>>>>>before queryWithVector the top is->{}", topK);
        List<QDRantQueryResult> rtnList = new ArrayList<>();
        try {
            VectorDbRepo vectorDbRepo = this.vectorDbRepoFactory.getVectorDbRepo(2);
            List<Float> promptV = vectorDbRepo.getEmbedding(inputPrompt);//取得用户的提示语的v
            Filter queryFilter = new Filter();
            //进入查询
            //先进行向量库查询
            rtnList = this.queryVectorsByPrompt(userName, repoId, promptV, queryFilter, topK);
        } catch (Exception e) {
            logger.error(">>>>>>getVectorSearchResult error->{}", e.getMessage(), e);
        }
        return rtnList;
    }

    private List<QDRantQueryResult> getVectorVLSearchResult(String userName, String repoId, String imgBase64Data,
                                                            int topK) {
        logger.info(">>>>>>before queryWithVector the top is->{}", topK);
        List<QDRantQueryResult> rtnList = new ArrayList<>();
        try {
            if (StringUtil.isBlank(imgBase64Data)) {
                return new ArrayList<>();
            }
            VectorDbRepo vectorDbRepo = this.vectorDbRepoFactory.getVectorDbRepo(3);
            List<Float> imgV = vectorDbRepo.getVlEmbedding("", imgBase64Data);
            Filter queryFilter = new Filter();
            //进入查询
            //先进行VL的向量库查询
            rtnList = this.queryVectorsVlByPrompt(userName, repoId, imgV, queryFilter, topK);
        } catch (Exception e) {
            logger.error(">>>>>>getVectorVLSearchResult error->{}", e.getMessage(), e);
        }
        return rtnList;
    }

    private RewritedQueryResult llmRewrite(String userName, String inputPrompt, UserChatConfigBean userChatConfigBean) {
        String rewritedPrompt = "";
        int chatType = 1;
        RewritedQueryResult rewritedQueryResult = new RewritedQueryResult(inputPrompt, 1);
        try {
            List<String> historyPrompts = this.chatHelper.getHistoryPrompt(userName, 5);
            String historyPromptsStr = objectMapper.writeValueAsString(historyPrompts);
            Map<String, String> paras = new HashMap<>();
            paras.put("userPrompt", inputPrompt);
            paras.put("historyPrompt", historyPromptsStr);
            String jsonResponse = this.difyTools.invokeWithBlocking(userName,
                                                                    userChatConfigBean.getRewriteDifySequenceNo(),
                                                                    paras);
            //String jsonResponse = this.safeJsonCall(userName, sendRequirement, masterModel, temperature);
            if (StringUtil.isNotBlank(jsonResponse)) {
                JSONObject jsonObject = new JSONObject(jsonResponse);
                JSONObject difyResult = jsonObject.getJSONObject("data")
                                                  .getJSONObject("outputs")
                                                  .getJSONObject("difyResult");
                rewritedPrompt = difyResult.getString("result");
                chatType = difyResult.getInt("chatType");
                rewritedQueryResult = new RewritedQueryResult(rewritedPrompt, chatType);
            } else {
                rewritedQueryResult = new RewritedQueryResult(inputPrompt, 1);//用AI折解提示语失败那么只能用原有提示语
            }
        } catch (Exception e) {
            logger.error(">>>>>>llm rewrite query master<->call error, use slave->{}", e.getMessage(), e);
        }
        return rewritedQueryResult;
    }

    private String safeJsonCall(String userName, String prompt, AIModel aiModel, double temperature) {
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

    private List<String> getLabelsFromInputPrompt(String userName, String inputPrompt,
                                                  double temperature) {
        String sendPrompt = "";
        List<String> labelPairs = new ArrayList<>();
        try {
            UserKnowledgeMain knowledgeMain = new UserKnowledgeMain();
            knowledgeMain = this.knowledgeVectorHelper.getUserKnowledgeMainById("67b795c4710dba0f7b7d1b31");
            List<String> systemLabelPairs = knowledgeMain.getLabelList();
            sendPrompt = this.aiFunctionHelper.getFunctions(20002, inputPrompt);
            sendPrompt = sendPrompt.replace("$<labels>", objectMapper.writeValueAsString(systemLabelPairs));
            AIModel masterModel = this.systemModelSupport.getMasterModel(userName);
            int masterModelType = masterModel.getType();
            logger.info(">>>>>>send prompt is->{}", sendPrompt);
            AIComponent aiComponent = this.aiFactory.getAIComponent(masterModelType);
            try {
                String jsonResponse = aiComponent.jsonCall(masterModel, sendPrompt, temperature);
                logger.info(">>>>>>得到AI打标签内容返回->{}", jsonResponse);
                JsonNode rootNode = objectMapper.readTree(jsonResponse);
                JsonNode targetNode = rootNode.has("keywords") ? rootNode.get("keywords") : rootNode;

                // 如果目标节点是数组，直接处理
                if (targetNode.isArray()) {
                    for (JsonNode labelNode : targetNode) {
                        // 如果节点是文本值，直接添加到列表中
                        if (labelNode.isTextual()) {
                            labelPairs.add(labelNode.asText());
                        }
                    }
                }
            } catch (Exception e) {
                logger.error(">>>>>>use master<->call erro, switch to use slave call json api->{}", e.getMessage(), e);
                try {
                    AIModel slaveModel = this.systemModelSupport.getSlave(userName);
                    if (slaveModel == null) {
                        //系统没有配slaveModel，那么直接就抛错了
                        logger.error(">>>>>>there is no slave<->call configured for lable user input prompt {}",
                                     e.getMessage(), e);
                        return new ArrayList<>();
                    }
                    int slaveModelType = slaveModel.getType();
                    logger.info(">>>>>>send prompt is->{}", sendPrompt);
                    aiComponent = this.aiFactory.getAIComponent(slaveModelType);
                    String jsonResponse = aiComponent.jsonCall(slaveModel, sendPrompt, temperature);
                    JSONObject jsonObject = new JSONObject(jsonResponse);
                    JsonNode rootNode = objectMapper.readTree(jsonResponse);
                    JsonNode targetNode = rootNode.has("keywords") ? rootNode.get("keywords") : rootNode;
                    // 如果目标节点是数组，直接处理
                    if (targetNode.isArray()) {
                        for (JsonNode labelNode : targetNode) {
                            // 如果节点是文本值，直接添加到列表中
                            if (labelNode.isTextual()) {
                                labelPairs.add(labelNode.asText());
                            }
                        }
                    }
                } catch (Exception se) {
                    logger.error(">>>>>>use slave<->call for label user input prompt error->{}", e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            logger.error(">>>>>>getLabelsFromInputPrompt service error->{}", e.getMessage(), e);
        }
        return labelPairs;
    }

    private List<QDRantQueryResult> queryVectorsByPrompt(String userName, String repoId, List<Float> promptV,
                                                         Filter queryFilter,
                                                         int topK) throws Exception {
        List<QDRantQueryResult> rtnList = new ArrayList<>();
        UserKnowledgeMain knowledgeMain = this.knowledgeVectorHelper.getUserKnowledgeMainById(repoId);
        String searchUrl = knowledgeMain.getKnowledgeName();
        VectorDbRepo vectorDbRepo = this.vectorDbRepoFactory.getVectorDbRepo(2);
        rtnList = vectorDbRepo.queryWithVector(searchUrl, promptV, queryFilter, topK);
        return rtnList;
    }

    private List<QDRantQueryResult> queryVectorsVlByPrompt(String userName, String repoId, List<Float> promptV,
                                                           Filter queryFilter,
                                                           int topK) throws Exception {
        List<QDRantQueryResult> rtnList = new ArrayList<>();
        UserKnowledgeMain knowledgeMain = this.knowledgeVectorHelper.getUserKnowledgeMainById(repoId);
        String searchUrl = knowledgeMain.getKnowledgeName() + "_vl";
        VectorDbRepo vectorDbRepo = this.vectorDbRepoFactory.getVectorDbRepo(2);
        rtnList = vectorDbRepo.queryWithVector(searchUrl, promptV, queryFilter, topK);
        return rtnList;
    }

    private void storeRetriveResultToMongo(String userName, String modelName, String originalPrompt,
                                           String rewritedPrompt, List<KnowledgeResult> retrieveResult) {
        String mongoCollectionName = "ChatRetrieveResult";
        ChatRetrieveResult chatRetrieveResult = new ChatRetrieveResult();
        chatRetrieveResult.setUserName(userName);
        chatRetrieveResult.setModelName(modelName);
        chatRetrieveResult.setOriginalPrompt(originalPrompt);
        chatRetrieveResult.setRewritedPrompt(rewritedPrompt);
        chatRetrieveResult.setRetrieveResult(retrieveResult);
        int recordCount = 0;
        float totalScore = 0;
        float averageScore = 0.0000f;
        try {
            if (retrieveResult != null && !retrieveResult.isEmpty()) {
                for (KnowledgeResult knowledgeResult : retrieveResult) {
                    recordCount += 1;
                    float score = knowledgeResult.getScore();
                    totalScore += score;  // 累加每条记录的分数
                }

// 使用 BigDecimal 计算并保留4位小数
                BigDecimal average = recordCount > 0
                        ? BigDecimal.valueOf(totalScore)
                                    .divide(BigDecimal.valueOf(recordCount), 4, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

                averageScore = average.floatValue();
            }
            chatRetrieveResult.setAverageScore(averageScore);
            mongoTemplate.save(chatRetrieveResult, mongoCollectionName);
        } catch (Exception e) {
            logger.error(">>>>>>save chat retrieve result into mongo error->{}", e.getMessage(), e);
        }
    }

}
