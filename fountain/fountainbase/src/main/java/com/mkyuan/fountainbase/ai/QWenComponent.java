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
public class QWenComponent implements AIComponent {
    protected Logger logger = LogManager.getLogger(this.getClass());
    private static final double DEFAULT_TEMPERATURE = 0.1;
    private static final String STREAM_DONE = "[DONE]";

    @Autowired
    private OkHttpHelper okHttpHelper;

    @Autowired
    private ChatRunningStatus chatRunningStatus;
    public String jsonCall(AIModel aiModel, String promptStr) throws Exception {
        return jsonCall(aiModel, promptStr, DEFAULT_TEMPERATURE);
    }

    @Override
    public String jsonCall(AIModel aiModel, RequestPayload requestPayload) throws Exception {
        try {
            String modelUrl = aiModel.getUrl();
            String modelName = aiModel.getModelName();
            String apiKeyValue = "Bearer " + aiModel.getApiKey();
            Map<String, String> headers = new HashMap<>();
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
            logger.info(">>>>>>use qwenvl2.5-72b call with RequestPayload json");
            String aiResponse = okHttpHelper.postJsonWithMultiHeaders(modelUrl, body, headers, 60000, 60000);
            JSONObject jsonObject = null;
            try {
                jsonObject = JSON.parseObject(aiResponse);
            } catch (Exception e) {
                this.logger.error("qwenvl2.5-72b JSON格式错误->{}", e.getMessage(), e);
                throw new IllegalStateException("qwenvl2.5-72b JSON格式错误");
            }
            this.logger.info(">>>>>>获取AI端响应->{}", aiResponse);

            JSONObject usage = jsonObject.getJSONObject("usage");
            JSONArray choices = jsonObject.getJSONArray("choices");
            String jsonResponseStr = "";
            JSONObject choicesObject = choices.getJSONObject(0);
            if (null != choicesObject && choicesObject.containsKey("message")) {
                JSONObject message = choicesObject.getJSONObject("message");
                jsonResponseStr = message.getString("content");
                jsonResponseStr = AIResponseUtil.extractJson(jsonResponseStr);
                return jsonResponseStr;
            } else {
                throw new Exception(">>>>>>AI端当前没有返回有效合法的内容，当前返回为->" + jsonResponseStr);
            }
        } catch (Exception e) {
            throw new Exception(">>>>>>use ALI BaiLian jsonCall with RequestPayload error->" + e.getMessage(), e);
        }
    }

    public String jsonCall(AIModel aiModel, String promptStr, double temperature) throws Exception {
        try {
            String modelUrl = aiModel.getUrl();
            String modelName = aiModel.getModelName();
            String apiKeyValue = "Bearer " + aiModel.getApiKey();
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("Authorization", apiKeyValue);
            Map<String, Object> body = new HashMap<>();
            body.put("model", modelName);
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
            logger.info(">>>>>>use qwenvl2.5-72b call json");
            String aiResponse = okHttpHelper.postJsonWithMultiHeaders(modelUrl, body, headers, 15000, 15000);
            JSONObject jsonObject = null;
            try {
                jsonObject = JSON.parseObject(aiResponse);
            } catch (Exception e) {
                this.logger.error("qwenvl2.5-72b JSON格式错误->{}", e.getMessage(), e);
                throw new IllegalStateException("qwenvl2.5-72b JSON格式错误");
            }
            this.logger.info(">>>>>>获取AI端响应->{}", aiResponse);

            JSONObject usage = jsonObject.getJSONObject("usage");
            JSONArray choices = jsonObject.getJSONArray("choices");
            String jsonResponseStr = "";
            JSONObject choicesObject = choices.getJSONObject(0);
            if (null != choicesObject && choicesObject.containsKey("message")) {
                JSONObject message = choicesObject.getJSONObject("message");
                jsonResponseStr = message.getString("content");
                jsonResponseStr = AIResponseUtil.extractJson(jsonResponseStr);
                return jsonResponseStr;
            } else {
                throw new Exception(">>>>>>AI端当前没有返回有效合法的内容，当前返回为->" + jsonResponseStr);
            }
        } catch (Exception e) {
            throw new Exception(">>>>>>use qwenvl2.5-72b jsonCall error->" + e.getMessage(), e);
        }
    }

    @Override
    public CompleteResponse doRequestWithStream(String userName, AIModel aiModel, RequestPayload request, CompleteCallback callback) {
        JSONObject body = new JSONObject();
        body.put("model", aiModel.getModelName());
        //body.put("input", Map.of("messages", request.getMessages()));
        body.put("messages", request.getMessages());
        //body.put("parameters", params);
        body.put("temperature", request.getTemperature());
        body.put("stream", true);
        OkHttpClient httpClient = new OkHttpClient.Builder()//
                                                            .readTimeout(0, TimeUnit.MILLISECONDS) // 0表示不超时
                                                            .build();
        //logger.info(">>>>>>body->{}",JSON.toJSONString(body, SerializerFeature.PrettyFormat));
        RequestBody requestBody = RequestBody.create(body.toJSONString(), MediaType.parse("application/json"));
        Request httpRequest = new Request.Builder()//
                                                   .url(aiModel.getUrl())//
                                                   .post(requestBody)//
                                                   .addHeader("Accept", "text/event-stream")//
                                                   .addHeader("Content-Type", "application/json")//
                                                   //.addHeader("X-DashScope-SSE", "enable")//
                                                   .addHeader("Authorization",
                                                              String.format("Bearer %s", aiModel.getApiKey()))//
                                                   .build();
        StringBuilder sb = new StringBuilder();
        try {
            RealEventSource realEventSource = new RealEventSource(httpRequest, new EventSourceListener() {

                @Override
                public void onOpen(EventSource eventSource, Response response) {
                }

                @Override
                public void onEvent(EventSource eventSource, String id, String type, String data) {
                    logger.info(">>>>>>QwenComponent-data->{}",data);
                    CompleteResponseChunk chunk=new CompleteResponseChunk();
                    if(!chatRunningStatus.isRunning(userName)){
                        eventSource.cancel();
                        return;
                    }else {
                        chunk = responseStream(data);
                        if (null != chunk) {
                            sb.append(chunk.getContent());
                            callback.onData(chunk);
                            //totalTokens.set(chunk.getTotalTokens());
                            //inputTokens.set(chunk.getPromptTokens());
                            //outputTokens.set(chunk.getCompletionTokens());
                        }
                    }
                }

                @Override
                public void onClosed(EventSource eventSource) {
                    callback.onDone();
                }

                @Override
                public void onFailure(EventSource eventSource, Throwable t, Response response) {
                    try {
                        logger.error(">>>>>>qwen聊天访问失败->{}", response.body().string());
                    } catch (Exception e) {
                    }
                    if (callback != null) {
                        callback.onError(new Exception(t));
                    }
                }
            });
            realEventSource.connect(httpClient);// 真正开始请求的一步
            CompleteResponse response = new CompleteResponse();
            response.setContent(sb.toString());
            //response.setPromptTokens(inputTokens.get());
            //response.setCompletionTokens(outputTokens.get());
            //response.setTotalTokens(totalTokens.get());
            logger.info(">>>>>>收到报文内容:{}", response.getContent());
            //logger.info(">>>>>>使用token数量:{} + {} = {}", response.getPromptTokens(), response.getCompletionTokens(), response.getTotalTokens());
            return response;
        } catch (Exception e) {
            logger.error(">>>>>>Qwen component call error->{}", e.getMessage());
            if (callback != null) {
                callback.onError(new Exception(e));
            }
        }
        return null;
    }

    private CompleteResponseChunk responseStream(String data) {
        //System.err.println(data);
        if (data.indexOf(STREAM_DONE) >= 0) {
            // 流式消息结果
            return null;
        }
        JSONObject message = JSON.parseObject(data);
        // 消息碎片,取出消息内容
        if (!message.containsKey("choices")) {
            // 不存在choices,不做处理
            return null;
        }
        JSONArray choices = message.getJSONArray("choices");
        if (null == choices || choices.size() == 0) {
            // choices 消息内容为空
            return null;
        }
        JSONObject target = choices.getJSONObject(0);
        if (null == target || (!target.containsKey("delta"))) {
            return null;
        }
        String content = target.getJSONObject("delta").getString("content");
        if (null == content) {
            return null;
        }
        CompleteResponseChunk chunk = new CompleteResponseChunk();
        chunk.setContent(content);
        return chunk;
    }
}
