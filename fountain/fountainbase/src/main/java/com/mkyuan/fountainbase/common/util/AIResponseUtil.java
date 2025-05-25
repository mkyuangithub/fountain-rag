package com.mkyuan.fountainbase.common.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AIResponseUtil {
    private static Logger logger = LogManager.getLogger(AIResponseUtil.class);

    public static String extraceSaasDeepSeek(String jsonResponse){
        JSONObject jsonObject = new JSONObject(jsonResponse);
        JSONObject choices = jsonObject.getJSONArray("choices").getJSONObject(0);
        JSONObject message = choices.getJSONObject("message");
        String content = message.getString("content");
        // 如果没有<think>标签，直接返回content
        return content;
    }
    public static String extractDeepSeekLabels(String jsonResponse) {
        // 先获取content部分
        JSONObject jsonObject = new JSONObject(jsonResponse);
        JSONObject message = jsonObject.getJSONObject("message");
        String content = message.getString("content");

        // 检查是否包含<think>标签
        if (content.contains("<think>")) {
            return content.replaceAll("<think>[\\s\\S]*?</think>\\n\\n", "");
        }
        // 如果没有<think>标签，直接返回content
        return content;
    }

    public static String extractAMDDeepSeekLabels(String jsonResponse) {
        // 先获取content部分
        JSONObject jsonObject = new JSONObject(jsonResponse);
        JSONObject message = jsonObject.getJSONObject("message");
        String content = message.getString("content");

        // 检查是否包含<think>标签
        if (content.contains("<think>")) {
            return content.replaceAll("<think>[\\s\\S]*?</think>\\n\\n", "");
        }
        // 如果没有<think>标签，直接返回content
        return content;
    }

    public static String extractJson(String input) {
        // 修改正则表达式以只匹配json标记，忽略大小写
        Pattern pattern = Pattern.compile("```(?i:json)\\s*(.*?)\\s*```", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            logger.info(">>>>>>搜索输入是否包含在```json开头和```结尾中间的内容，结果为->找不到，返回原始input");
            return input;
        }
    }

}
