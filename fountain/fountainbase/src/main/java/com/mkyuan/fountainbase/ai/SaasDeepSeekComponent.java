package com.mkyuan.fountainbase.ai;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mkyuan.fountainbase.agent.chatbot.service.ChatRunningStatus;
import com.mkyuan.fountainbase.ai.bean.AIModel;
import com.mkyuan.fountainbase.ai.bean.RequestPayload;
import com.mkyuan.fountainbase.common.util.AIResponseUtil;
import com.mkyuan.fountainbase.common.util.okhttp.OkHttpHelper;
import okhttp3.*;
import okhttp3.internal.sse.RealEventSource;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class SaasDeepSeekComponent implements AIComponent {
    protected Logger logger = LogManager.getLogger(this.getClass());
    private static final double DEFAULT_TEMPERATURE = 0.1;

    @Autowired
    private OkHttpHelper okHttpHelper;

    @Autowired
    private ChatRunningStatus chatRunningStatus;

    @Override
    public String jsonCall(AIModel aiModel, String promptStr) throws Exception {
        return this.jsonCall(aiModel, promptStr, DEFAULT_TEMPERATURE);
    }

    @Override
    public String jsonCall(AIModel aiModel, RequestPayload requestPayload) throws Exception {
        String responseStr = "";
        try {
            String modelUrl = aiModel.getUrl();
            String modelName = aiModel.getModelName();
            Map<String, String> headers = new HashMap<>();
            String apiKeyValue = "Bearer " + aiModel.getApiKey();
            headers.put("Content-Type", "application/json");
            headers.put("Authorization", apiKeyValue);
            Map<String, Object> body = new HashMap<>();
            //body.put("model", model);
            //body.put("input", Map.of("messages", request.getMessages()));
            body.put("messages", requestPayload.getMessages());
            //body.put("parameters", params);
            body.put("temperature", requestPayload.getTemperature());
            body.put("stream", false);
            body.put("top_p", requestPayload.getTop_p());
            body.put("model", requestPayload.getModel());
            logger.info(">>>>>>use SaasDeepSeekComponent call with RequestPayload json");
            responseStr = okHttpHelper.postJsonWithMultiHeaders(modelUrl, body, headers, 60000, 60000);
            String cleanResult = AIResponseUtil.extraceSaasDeepSeek(responseStr);
            cleanResult = AIResponseUtil.extractJson(cleanResult);
            return cleanResult;
        } catch (Exception e) {
            throw new Exception(
                    ">>>>>>use SaasDeepSeekComponent jsonCall with RequestPayload error->{}" + e.getMessage(), e);
        }
    }

    @Override
    public String jsonCall(AIModel aiModel, String promptStr, double temperature) throws Exception {
        String responseStr = "";
        try {
            String modelUrl = aiModel.getUrl();
            String modelName = aiModel.getModelName();
            Map<String, String> headers = new HashMap<>();
            String apiKeyValue = "Bearer " + aiModel.getApiKey();
            headers.put("Content-Type", "application/json");
            headers.put("Authorization", apiKeyValue);
            Map<String, Object> body = new HashMap<>();
            //body.put("model", model);
            //body.put("input", Map.of("messages", request.getMessages()));
            Map<String, String> queryMessage = new HashMap<>();
            queryMessage.put("role", "user");
            queryMessage.put("content", promptStr);
            List<Object> messages = new ArrayList<>();
            messages.add(queryMessage);
            body.put("messages", messages);
            //body.put("parameters", params);
            body.put("temperature", temperature);
            body.put("stream", Boolean.valueOf("false"));
            body.put("top_p", Double.parseDouble("0.95"));
            body.put("model", modelName);
            responseStr = okHttpHelper.postJsonWithMultiHeaders(modelUrl, body, headers, 60000, 60000);
            String cleanResult = AIResponseUtil.extraceSaasDeepSeek(responseStr);
            cleanResult = AIResponseUtil.extractJson(cleanResult);
            return cleanResult;
        } catch (Exception e) {
            throw new Exception(">>>>>>use SaasDeepSeekComponent jsonCall error->{}" + e.getMessage(), e);
        }
    }

    @Override
    public CompleteResponse doRequestWithStream(String userName, AIModel aiModel, RequestPayload request,
                                                CompleteCallback callback) {
        JSONObject body = new JSONObject();
        //body.put("model", model);
        //body.put("input", Map.of("messages", request.getMessages()));
        body.put("messages", request.getMessages());
        //body.put("parameters", params);
        body.put("temperature", request.getTemperature());
        body.put("stream", true);
        body.put("top_p", request.getTop_p());
        body.put("model", aiModel.getModelName());
        OkHttpClient httpClient = new OkHttpClient.Builder()//
                                                            .readTimeout(0, TimeUnit.MILLISECONDS) // 0表示不超时
                                                            .build();
        RequestBody requestBody = RequestBody.create(body.toJSONString(), MediaType.parse("application/json"));
        String apiKeyValue = "Bearer " + aiModel.getApiKey();
        Request httpRequest = new Request.Builder()//
                                                   .url(aiModel.getUrl())//
                                                   .post(requestBody)//
                                                   .addHeader("Accept", "text/event-stream")//
                                                   .addHeader("Content-Type", "application/json")//
                                                   //.addHeader("X-DashScope-SSE", "enable")//
                                                   .addHeader("Authorization", apiKeyValue)
                                                   .build();
        StringBuilder sb = new StringBuilder();
        try {
            RealEventSource realEventSource = new RealEventSource(httpRequest, new EventSourceListener() {

                @Override
                public void onOpen(EventSource eventSource, Response response) {
                }

                @Override
                public void onEvent(EventSource eventSource, String id, String type, String data) {
                    if (data.equals("[DONE]")) {
                        return;
                    }
                    CompleteResponseChunk chunk = new CompleteResponseChunk();
                    if (!chatRunningStatus.isRunning(userName)) {
                        eventSource.cancel();
                        return;
                    } else {
                        chunk = responseStream(data);
                        if (null != chunk) {
                            // 只有当content不为null时才追加到StringBuilder
                            if (chunk.getContent() != null) {
                                sb.append(chunk.getContent());
                            }

                            // 创建包含实际数据的JSON对象
                            JSONObject responseData = new JSONObject();
                            responseData.put("data", chunk.getContent());  // 保持原有的data字段
                            if (chunk.getThink() != null) {
                                responseData.put("think", chunk.getThink());  // 添加新的think字段
                            }

                            // 将处理后的数据传给回调
                            callback.onData(chunk);

                        }
                    }
                }

                @Override
                public void onClosed(EventSource eventSource) {
                    callback.onDone();
                }

                @Override
                public void onFailure(EventSource eventSource, Throwable t, Response response) {
                    callback.onError(new Exception(t));
                }
            });
            realEventSource.connect(httpClient);// 真正开始请求的一步
            CompleteResponse response = new CompleteResponse();
            response.setContent(sb.toString());
            //response.setPromptTokens(inputTokens.get());
            //response.setCompletionTokens(outputTokens.get());
            //response.setTotalTokens(totalTokens.get());
            logger.info(">>>>>>收到deep seek报文内容:{}", response.getContent());
            //logger.info(">>>>>>使用token数量:{} + {} = {}", response.getPromptTokens(), response.getCompletionTokens(), response.getTotalTokens());
            return response;
        } catch (Exception e) {
            logger.error(">>>>>>SaasDeepSeek call error->{}", e.getMessage(), e);
            if (callback != null) {
                callback.onError(new Exception(e));
            }
        }
        return null;
    }

    private CompleteResponseChunk responseStream(String data) {
        JSONObject message = JSON.parseObject(data);

        // 检查choices是否存在
        if (!message.containsKey("choices")) {
            return null;
        }

        JSONArray choices = message.getJSONArray("choices");
        if (null == choices || choices.size() == 0) {
            return null;
        }

        JSONObject choice = choices.getJSONObject(0);
        if (null == choice || !choice.containsKey("delta")) {
            return null;
        }

        JSONObject delta = choice.getJSONObject("delta");
        String content = delta.getString("content");
        String reasoningContent = delta.getString("reasoning_content");

        CompleteResponseChunk chunk = new CompleteResponseChunk();
        // 设置 content，即使为 null 也设置
        chunk.setContent(content);

        // 只有当 reasoning_content 不为 null 且不为空时才设置
        if (reasoningContent != null && !reasoningContent.isEmpty()) {
            chunk.setThink(reasoningContent);
        }

        // 只有当两者都为空时才返回 null
        if ((content == null || content.isEmpty()) &&
                (reasoningContent == null || reasoningContent.isEmpty())) {
            return null;
        }

        return chunk;
    }
}
