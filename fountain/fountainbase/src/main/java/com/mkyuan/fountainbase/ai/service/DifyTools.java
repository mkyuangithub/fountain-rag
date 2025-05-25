package com.mkyuan.fountainbase.ai.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mkyuan.fountainbase.agent.chatbot.bean.RewritedQueryResult;
import com.mkyuan.fountainbase.agent.chatbot.service.ChatRunningStatus;
import com.mkyuan.fountainbase.ai.CompleteCallback;
import com.mkyuan.fountainbase.ai.CompleteResponse;
import com.mkyuan.fountainbase.ai.CompleteResponseChunk;
import com.mkyuan.fountainbase.ai.bean.AIModel;
import com.mkyuan.fountainbase.ai.bean.DifyBean;
import com.mkyuan.fountainbase.ai.bean.RequestPayload;
import com.mkyuan.fountainbase.common.util.EncryptUtil;
import com.mkyuan.fountainbase.common.util.okhttp.OkHttpHelper;
import jodd.util.StringUtil;
import okhttp3.*;
import okhttp3.internal.sse.RealEventSource;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class DifyTools {
    protected Logger logger = LogManager.getLogger(this.getClass());
    private ObjectMapper objectMapper = new ObjectMapper();

    @Value("${fountain.secretKey}")
    private String secretKey = "";

    @Value("${fountain.ai.dify.workflowUrl}")
    private String workflowUrl = "";

    @Value("${fountain.ai.dify.chatflowUrl}")
    private String chatflowUrl = "";

    @Autowired
    private DifyService difyService;

    @Autowired
    private OkHttpHelper okHttpHelper;

    @Autowired
    private DifyHelper difyHelper;

    @Autowired
    private ChatRunningStatus chatRunningStatus;

    public String invokeWithBlocking(String userName, String difySequenceNo, Map<String, String> stringrizedParas) {
        String result = "";
        try {
            DifyBean difyBean = this.difyService.findDifyConfigBySequenceNo(userName, difySequenceNo);
            if (difyBean == null) {
                return "";
            }
            //组装dify的workflow需要的api调用结构
            JSONObject payload = new JSONObject();
            JSONObject inputs = new JSONObject();
            for (Map.Entry<String, String> entry : stringrizedParas.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                inputs.put(key, value);
            }
            payload.put("inputs", inputs);
            payload.put("response_mode", difyBean.getResponseMode());
            payload.put("user", difyBean.getUser());
            String apiKey = EncryptUtil.decrypt_safeencode(difyBean.getApiKey(), secretKey);
            String headerApiKeyValue = "Bearer " + apiKey;
            Map<String, String> header = new HashMap<>();
            header.put("Content-Type", "application/json");
            header.put("Authorization", headerApiKeyValue);
            String jsonResponse = this.okHttpHelper.postJsonWithMultiHeaders(workflowUrl, payload, header);
            // 解码Unicode
            String decodedResponse = StringEscapeUtils.unescapeJava(jsonResponse);
            logger.info(">>>>>>invokeWithBlocking from dify result->{}", decodedResponse);
            return decodedResponse;
        } catch (Exception e) {
            logger.error(">>>>>>invokeWithBlocking error->{}", e.getMessage(), e);
        }
        return result;
    }

    public void cleanAllSession(String userName,String chatDifySequence){
        logger.info(">>>>>>into clearnAllSession");
        JSONArray conversationList=this.difyHelper.getAllConversation(userName,chatDifySequence);
        logger.info(">>>>>>conversationList size->{}",conversationList.size());
        if(conversationList!=null&&!conversationList.isEmpty()){
            this.difyHelper.deleteAllConversation(userName,chatDifySequence,conversationList);
        }
    }
    public CompleteResponse invokeWithStream(String userName, String conversationId, String sendPrompt,
                                                String difySequenceNo, Map<String, String> stringrizedParas,
                                                CompleteCallback callback) {
        DifyBean difyBean = this.difyService.findDifyConfigBySequenceNo(userName, difySequenceNo);
        if (difyBean == null) {
            return new CompleteResponse();
        }
        try {
            JSONObject payload = new JSONObject();
            JSONObject inputs = new JSONObject();
            for (Map.Entry<String, String> entry : stringrizedParas.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                inputs.put(key, value);
            }
            payload.put("inputs", inputs);
            payload.put("query", sendPrompt);
            payload.put("response_mode", difyBean.getResponseMode());
            payload.put("user", userName);
            if (StringUtil.isNotBlank(conversationId)) {
                payload.put("conversation_id", conversationId);
            }
            String apiKey = EncryptUtil.decrypt_safeencode(difyBean.getApiKey(), secretKey);
            String headerApiKeyValue = "Bearer " + apiKey;
            OkHttpClient httpClient = new OkHttpClient.Builder()//
                                                                .readTimeout(0, TimeUnit.MILLISECONDS) // 0表示不超时
                                                                .build();
            RequestBody requestBody = RequestBody.create(payload.toJSONString(), MediaType.parse("application/json"));
            Request httpRequest = new Request.Builder()//
                                                       .url(chatflowUrl)//
                                                       .post(requestBody)//
                                                       .addHeader("Accept", "text/event-stream")//
                                                       .addHeader("Content-Type", "application/json")//
                                                       .addHeader("Authorization", headerApiKeyValue)
                                                       .build();
            StringBuilder sb = new StringBuilder();
            RealEventSource realEventSource = new RealEventSource(httpRequest, new EventSourceListener() {

                @Override
                public void onOpen(EventSource eventSource, Response response) {
                }

                @Override
                public void onEvent(EventSource eventSource, String id, String type, String data) {
                    try {
                        String decodedResponse = StringEscapeUtils.unescapeJava(data);
                        logger.info(">>>>>>data->{}", decodedResponse);
                        CompleteResponseChunk chunk = new CompleteResponseChunk();
                        if (!chatRunningStatus.isRunning(userName)) {
                            eventSource.cancel();
                            return;
                        } else {
                            chunk = responseStream(data);
                            if (null != chunk) {
                                sb.append(chunk.getContent());
                                callback.onData(chunk);
                                //totalTokens.set(chunk.getTotalTokens());
                                //inputTokens.set(chunk.getPromptTokens());
                                //outputTokens.set(chunk.getCompletionTokens());
                            }
                        }
                    } catch (Exception e) {
                        logger.error("local dify 处理SSE事件数据失败", e);
                    }
                }

                @Override
                public void onClosed(EventSource eventSource) {
                    callback.onDone();
                }

                @Override
                public void onFailure(EventSource eventSource, Throwable t, Response response) {
                    try {
                        String responseBody = response.body().string();
                        logger.info(">>>>>>Response body->{}", responseBody);

                        // 尝试处理响应体中的数据
                        if (responseBody != null && !responseBody.isEmpty()) {
                            // 处理可能的多行JSON数据
                            String[] jsonLines = responseBody.split("\n");
                            boolean hasValidData = false;

                            for (String line : jsonLines) {
                                if (line.trim().isEmpty()) {
                                    continue;
                                }

                                try {
                                    CompleteResponseChunk chunk = responseStream(line);
                                    if (chunk != null) {  // 包括空内容的结束chunk
                                        sb.append(chunk.getContent());
                                        callback.onData(chunk);
                                        hasValidData = true;

                                        // 如果是结束消息，触发完成回调
                                        if (line.contains("\"done\":true")) {
                                            callback.onDone();
                                            return;
                                        }
                                    }
                                } catch (Exception lineError) {
                                    logger.warn("处理单行数据失败: {}", line, lineError);
                                    // 继续处理下一行
                                }
                            }

                            // 如果成功处理了任何数据，就不触发错误回调
                            if (hasValidData) {
                                return;
                            }
                        }

                        // 只有在没有成功处理任何数据的情况下才记录错误
                        logger.error(">>>>>>phi4聊天访问失败", t);
                        callback.onError(new Exception(t));
                    } catch (Exception e) {
                        logger.error("处理失败响应时发生错误", e);
                        callback.onError(new Exception(t));
                    } finally {
                        try {
                            response.close();
                        } catch (Exception e) {
                        }
                    }
                }
            });
            realEventSource.connect(httpClient);// 真正开始请求的一步
            CompleteResponse response = new CompleteResponse();
            response.setContent(sb.toString());
            //response.setPromptTokens(inputTokens.get());
            //response.setCompletionTokens(outputTokens.get());
            //response.setTotalTokens(totalTokens.get());
            logger.info(">>>>>>收到dify报文内容:{}", response.getContent());
            //logger.info(">>>>>>使用token数量:{} + {} = {}", response.getPromptTokens(), response.getCompletionTokens(), response.getTotalTokens());
            return response;
        } catch (Exception e) {
            logger.error(">>>>>>dify call error->{}", e.getMessage(), e);
            if (callback != null) {
                callback.onError(new Exception(e));
            }
        }
        return null;
    }

    private boolean isRecordingThink = false; // 用于跟踪是否在记录 <think> 内容

    private CompleteResponseChunk responseStream(String data) {
        try {
            // 预处理：检查数据是否完整的JSON
            if (!data.endsWith("}")) {
                logger.warn("Incomplete JSON data received: {}", data);
                return null;
            }

            JSONObject message = JSON.parseObject(data);
            CompleteResponseChunk chunk = new CompleteResponseChunk();

            // 检查是否是 node_finished 事件
            if ("node_finished".equals(message.getString("event"))) {
                JSONObject dataObj = message.getJSONObject("data");
                if (dataObj != null && "masterDataTool".equals(dataObj.getString("title"))) {
                    // 获取 outputs 中的 structured_output
                    JSONObject outputs = dataObj.getJSONObject("outputs");
                    if (outputs != null) {
                        JSONObject structuredOutput = outputs.getJSONObject("structured_output");
                        if (structuredOutput != null) {
                            // 获取 productIds 数组
                            JSONArray productIdsArray = structuredOutput.getJSONArray("productIds");
                            if (productIdsArray != null) {
                                List<String> productIds = new ArrayList<>();
                                for (int i = 0; i < productIdsArray.size(); i++) {
                                    productIds.add(productIdsArray.getString(i));
                                }
                                chunk.setProductIds(productIds);
                            }
                        }
                    }
                }
            }

            // 原有的消息处理逻辑
            if (message.containsKey("answer")) {
                String answer = message.getString("answer");
                if (answer != null) {
                    chunk.setContent(answer);
                }
            }

            // 设置conversation_id
            if (message.containsKey("conversation_id")) {
                chunk.setConversationId(message.getString("conversation_id"));
            }

            return chunk;
        } catch (Exception e) {
            logger.error("处理SSE数据失败: {}", data, e);
            return null;
        }
    }

}
