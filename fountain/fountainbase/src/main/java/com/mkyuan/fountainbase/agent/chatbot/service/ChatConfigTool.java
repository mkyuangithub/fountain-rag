package com.mkyuan.fountainbase.agent.chatbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mkyuan.fountainbase.agent.chatbot.bean.ChatConfigDetail;
import com.mkyuan.fountainbase.agent.chatbot.bean.ChatConfigMain;
import com.mkyuan.fountainbase.agent.chatbot.bean.UserChatConfigBean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatConfigTool {

    protected Logger logger = LogManager.getLogger(this.getClass());
    private ObjectMapper objectMapper = new ObjectMapper();


    @Autowired
    ChatConfigHelper chatConfigHelper;

    //得到一个chat config的全局设定
    public UserChatConfigBean getUserChatConfig(String userName,long configMainId){
        UserChatConfigBean userChatConfigBean=new UserChatConfigBean();
        ChatConfigMain configMainInfo=this.chatConfigHelper.getChatConfigMainById(userName,configMainId);
        //List<ChatConfigDetail> detailConfigInfo=this.chatConfigHelper.getChatConfigDetailByMainId(userName, configMainId);
        userChatConfigBean.setConfigMainId(configMainInfo.getId());
        userChatConfigBean.setSystemMsg(configMainInfo.getSystemMsg());
        userChatConfigBean.setGlobalTemperature(configMainInfo.getTemperature());
        userChatConfigBean.setGroovyRules(configMainInfo.getGroovyRules());
        userChatConfigBean.setAllowUsers(configMainInfo.getAllowUsers());
        userChatConfigBean.setKnowledgeRepoIdList(configMainInfo.getKnowledgeRepoIdList());
        userChatConfigBean.setRewriteDifySequenceNo(configMainInfo.getRewriteSelectedDifySequenceNo());
        userChatConfigBean.setChatDifySequenceNo(configMainInfo.getChatSelectedDifySequenceNo());
        //userChatConfigBean.setDetailList(detailConfigInfo);
        return userChatConfigBean;
    }

    //得到一个chat config里具体某个步骤（type1-5)的设定
    public ChatConfigDetail getUserChatConfigDetailStep(String userName,long configMainId,int stepType){
        List<ChatConfigDetail> detailList=this.chatConfigHelper.getChatConfigDetailByMainId(userName,configMainId);
        if(detailList!=null || !detailList.isEmpty()){
            for(ChatConfigDetail detailItem: detailList){
                if(detailItem.getType()==stepType){
                    return detailItem;
                }
            }
        }
        return null;
    }
}
