package com.mkyuan.fountainbase.ai;

import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CompleteResponseChunk {

    private String content;
    private String think;  // 新增字段用于存储 reasoning_content

    private Integer durationMilliseconds;
    private Integer completionTokens;
    private Integer promptTokens;
    private Integer totalTokens;
    private String conversationId;  // 新增字段
    private List<String> productIds=new ArrayList<>();  // 新增字段

    public List<String> getProductIds() {
        return productIds;
    }

    public void setProductIds(List<String> productIds) {
        this.productIds.clear();
        this.productIds.addAll(productIds);
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getContent() {
        return this.content;
    }
    public String cleanMarkdownCode(String value) {
        if (value == null) return null;

        // 先移除带有语言标识的代码块标记
        // (?m) 启用多行模式
        // ^ 匹配行的开始
        // ```\s*(html|json|plaintext|\w*)\s*$ 匹配整行的代码块开始标记
        value = value.replaceAll("(?m)^```\\s*(html|json|plaintext|\\w*)\\s*$", "");

        // 再移除单独的 ``` 标记
        value = value.replaceAll("(?m)^```\\s*$", "");

        return value.trim();
    }

    public String getContent(boolean clear){
        String value = this.content;
        if(clear) {
            //value = value.replace("```html","");
            //value = value.replace("```json","");
            //value = value.replace("```plaintext","");
            //value = value.replace("```","");
            value=this.cleanMarkdownCode(value);
            value = value.replaceAll("\\[\\d+\\]", "");
        }
        return value;
    }
    public String getJsonContent(boolean clear){
        String value = this.content;
        if(clear) {
            value = value.replace("```html","");
            value = value.replace("```json","");
            value = value.replace("```","");
        }
        JSONObject obj = new JSONObject();
        obj.put("data",value);
        return obj.toJSONString();
    }
    public String getJsonContentWithNoRef(boolean clear){
    	String value = this.content;
    	if(clear) {
    		value = value.replace("```html","");
    		value = value.replace("```json","");
            value = value.replace("```plaintext","");
    		value = value.replace("```","");
            // 使用正则表达式移除[数字]格式
            value = value.replaceAll("\\[\\d+\\]", "");
    	}
        JSONObject obj = new JSONObject();
        obj.put("data",value);
        return obj.toJSONString();
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getDurationMilliseconds() {
        return durationMilliseconds;
    }

    public void setDurationMilliseconds(Integer durationMilliseconds) {
        this.durationMilliseconds = durationMilliseconds;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(Integer completionTokens) {
        this.completionTokens = completionTokens;
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(Integer promptTokens) {
        this.promptTokens = promptTokens;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Integer totalTokens) {
        this.totalTokens = totalTokens;
    }
    public String getThink() {
        return think;
    }
    public String getThinkContent(boolean clear){
        String value = this.think;
        if(clear) {
            //value = value.replace("```plaintext","");
            //value = value.replace("```html","");
            //value = value.replace("```json","");
            //value = value.replace("```","");

            // 使用正则表达式移除[数字]格式
            //value = value.replaceAll("\\[\\d+\\]", "");
            value = value.replaceAll("\\[\\d+\\]", "");
            value=this.cleanMarkdownCode(value);
        }
        //JSONObject obj = new JSONObject();
        //obj.put("think",value);
        //return obj.toJSONString();
        return value;
    }

    public void setThink(String think) {
        this.think = think;
    }
}