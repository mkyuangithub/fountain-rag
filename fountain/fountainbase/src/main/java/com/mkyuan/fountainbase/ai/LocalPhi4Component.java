package com.mkyuan.fountainbase.ai;

import com.alibaba.fastjson.JSON;
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
public class LocalPhi4Component implements AIComponent {
    protected Logger logger = LogManager.getLogger(this.getClass());
    private static final double DEFAULT_TEMPERATURE = 0.1;

    @Autowired
    private OkHttpHelper okHttpHelper;

    @Autowired
    private ChatRunningStatus chatRunningStatus;

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
            headers.put("Content-Type", "application/json");
            headers.put("api-key", aiModel.getApiKey());
            Map<String, Object> body = new HashMap<>();
            //body.put("model", model);
            //body.put("input", Map.of("messages", request.getMessages()));
            body.put("messages", requestPayload.getMessages());
            //body.put("parameters", params);
            body.put("temperature", requestPayload.getTemperature());
            body.put("stream", false);
            body.put("top_p", requestPayload.getTop_p());
            body.put("model", requestPayload.getModel());
            logger.info(">>>>>>use local phi4 call json with RequestPayload");
            responseStr = okHttpHelper.postJsonWithMultiHeaders(modelUrl, body, headers, 60000, 60000);
            String cleanResult = AIResponseUtil.extractDeepSeekLabels(responseStr);
            cleanResult = AIResponseUtil.extractJson(cleanResult);
            return cleanResult;
        } catch (Exception e) {
            throw new Exception(
                    ">>>>>>use local phi4 jsonCall with RequestPayload error->" + e.getMessage(), e);
        }
    }

    public String jsonCall(AIModel aiModel, String promptStr, double temperature) throws Exception {
        String responseStr = "";
        try {
            String modelUrl = aiModel.getUrl();
            String modelName = aiModel.getModelName();
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
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
            String cleanResult = AIResponseUtil.extractDeepSeekLabels(responseStr);
            cleanResult = AIResponseUtil.extractJson(cleanResult);
            return cleanResult;
        } catch (Exception e) {
            throw new Exception(">>>>>>use local phi4 jsonCall error->" + e.getMessage(), e);
        }
    }

    @Override
    public CompleteResponse doRequestWithStream(String userName, AIModel aiModel, RequestPayload request, CompleteCallback callback) {
        JSONObject body = new JSONObject();
        //body.put("model", model);
        //body.put("input", Map.of("messages", request.getMessages()));
        body.put("messages", request.getMessages());
        //body.put("parameters", params);
        body.put("temperature", request.getTemperature());
        body.put("stream", true);
        body.put("top_p", request.getTop_p());
        body.put("model", request.getModel());
        OkHttpClient httpClient = new OkHttpClient.Builder()//
                                                            .readTimeout(0, TimeUnit.MILLISECONDS) // 0表示不超时
                                                            .build();
        RequestBody requestBody = RequestBody.create(body.toJSONString(), MediaType.parse("application/json"));
        Request httpRequest = new Request.Builder()//
                                                   .url(aiModel.getUrl())//
                                                   .post(requestBody)//
                                                   .addHeader("Accept", "text/event-stream")//
                                                   .addHeader("Content-Type", "application/json")//
                                                   //.addHeader("X-DashScope-SSE", "enable")//
                                                   //.addHeader("xApiKey", perplexityAgentKey)//
                                                   .build();
        StringBuilder sb = new StringBuilder();
        try {
            RealEventSource realEventSource = new RealEventSource(httpRequest, new EventSourceListener() {

                @Override
                public void onOpen(EventSource eventSource, Response response) {
                }

                @Override
                public void onEvent(EventSource eventSource, String id, String type, String data) {
                    try {
                        //logger.info(">>>>>>data->{}", data);
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
                    } catch (Exception e) {
                        logger.error("local phi4 处理SSE事件数据失败", e);
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
            logger.info(">>>>>>收到phi4报文内容:{}", response.getContent());
            //logger.info(">>>>>>使用token数量:{} + {} = {}", response.getPromptTokens(), response.getCompletionTokens(), response.getTotalTokens());
            return response;
        }catch(Exception e){
            logger.error(">>>>>>phi4 call error->{}",e.getMessage(),e);
            if(callback!=null){
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

            // 检查是否结束消息
            if (message.containsKey("done") && message.getBooleanValue("done")) {
                // 如果是结束消息，创建一个空的chunk表示结束
                CompleteResponseChunk endChunk = new CompleteResponseChunk();
                // 可以选择设置一些统计信息
                if (message.containsKey("total_duration")) {
                    endChunk.setDurationMilliseconds(message.getInteger("total_duration") / 1000000); // 转换为毫秒
                }
                endChunk.setContent(""); // 设置空内容
                return endChunk;
            }

            if (!message.containsKey("message")) {
                return null;
            }

            JSONObject messageObj = message.getJSONObject("message");
            if (null == messageObj || !messageObj.containsKey("content")) {
                return null;
            }

            String content = messageObj.getString("content");
            if (null == content) {
                return null;
            }

            // 处理流式数据
            CompleteResponseChunk chunk = new CompleteResponseChunk();

            // 处理流式数据
            chunk.setContent(content);
            return chunk;
        } catch (Exception e) {
            logger.error("处理SSE数据失败: {}", data, e);
            return null;
        }
    }
}
