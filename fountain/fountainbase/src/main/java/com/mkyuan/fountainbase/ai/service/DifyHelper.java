package com.mkyuan.fountainbase.ai.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mkyuan.fountainbase.ai.bean.DifyBean;
import com.mkyuan.fountainbase.common.util.EncryptUtil;
import com.mkyuan.fountainbase.common.util.okhttp.OkHttpHelper;
import jodd.util.StringUtil;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class DifyHelper {

    protected Logger logger = LogManager.getLogger(this.getClass());
    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private OkHttpHelper okHttpHelper;

    @Autowired
    private DifyService difyService;

    @Value("${fountain.ai.dify.conversationUrl}")
    private String conversationUrl = "";


    @Value("${fountain.secretKey}")
    private String secretKey = "";

    public JSONArray getAllConversation(String userName, String difySequenceNo){
        JSONArray conversationList=new JSONArray();
        try{
            logger.info(">>>>>>getAllConversation user->{}, difySequenceNo->{}",userName,difySequenceNo);
            DifyBean difyBean = this.difyService.findDifyConfigBySequenceNo(userName, difySequenceNo);
            if (difyBean == null) {
                return new JSONArray();
            }

            String apiKey = EncryptUtil.decrypt_safeencode(difyBean.getApiKey(), secretKey);
            String headerApiKeyValue = "Bearer " + apiKey;
            Map<String, String> header = new HashMap<>();
            header.put("Content-Type", "application/json");
            header.put("Authorization", headerApiKeyValue);
            Map<String,String>httpParas=new HashMap<>();
            httpParas.put("user",userName);
            httpParas.put("limnit","20");
            String jsonResponse=this.okHttpHelper.getJson(conversationUrl,httpParas,header);
            String decodedResponse = StringEscapeUtils.unescapeJava(jsonResponse);
            logger.info(">>>>>>获取所有的会话列表结果->{}",decodedResponse);
            JSONObject jsonObject = JSON.parseObject(jsonResponse);
            if(StringUtil.isNotBlank(decodedResponse)) {
                // 获取data数组
                JSONArray dataArray = jsonObject.getJSONArray("data");
                if (dataArray != null) {
                    // 遍历数组
                    for (int i = 0; i < dataArray.size(); i++) {
                        JSONObject item = dataArray.getJSONObject(i);
                        String id = item.getString("id");
                        conversationList.add(id);
                    }
                }
            }
        }catch(Exception e){
            logger.error(">>>>>>getAllConversation error->{}",e.getMessage(),e);
        }
        return conversationList;
    }
    public void deleteAllConversation(String userName,String difySequenceNo, JSONArray conversationList){
        DifyBean difyBean = this.difyService.findDifyConfigBySequenceNo(userName, difySequenceNo);
        if (difyBean == null) {
            return;
        }
        try{
            String apiKey = EncryptUtil.decrypt_safeencode(difyBean.getApiKey(), secretKey);
            String headerApiKeyValue = "Bearer " + apiKey;
            Map<String, String> header = new HashMap<>();
            header.put("Content-Type", "application/json");
            header.put("Authorization", headerApiKeyValue);
            for (int i = 0; i < conversationList.size(); i++) {
                // 直接获取String，因为之前add的就是String类型的id
                String conversationId = conversationList.getString(i);
               String deleteUrl=conversationUrl+"/"+conversationId;
               JSONObject body=new JSONObject();
               body.put("user",userName);
               String jsonResponse=this.okHttpHelper.delete(deleteUrl,header,body,5000,5000);
               logger.info(">>>>>>deleteconversation->{}", jsonResponse);
            }
        }catch(Exception e){
            logger.error(">>>>>>deleteAllConversation error->{}",e.getMessage(),e);
        }
    }
}
