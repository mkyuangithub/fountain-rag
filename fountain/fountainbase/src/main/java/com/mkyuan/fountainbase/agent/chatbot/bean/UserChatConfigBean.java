package com.mkyuan.fountainbase.agent.chatbot.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class UserChatConfigBean implements Serializable {

    private long configMainId=0l;
    private double globalTemperature=0.1;
    private String systemMsg="";
    private String groovyRules="";
    private List<String> knowledgeRepoIdList=new ArrayList<>();
    private List<String> allowUsers=new ArrayList<>();
    private List<ChatConfigDetail> detailList=new ArrayList<>();
    private String rewriteDifySequenceNo="";
    private String chatDifySequenceNo="";
    private String description="";
    private String userName="";
    private String createdBy="";

    public String getRewriteDifySequenceNo() {
        return rewriteDifySequenceNo;
    }

    public void setRewriteDifySequenceNo(String rewriteDifySequenceNo) {
        this.rewriteDifySequenceNo = rewriteDifySequenceNo;
    }

    public String getChatDifySequenceNo() {
        return chatDifySequenceNo;
    }

    public void setChatDifySequenceNo(String chatDifySequenceNo) {
        this.chatDifySequenceNo = chatDifySequenceNo;
    }

    public String getGroovyRules() {
        return groovyRules;
    }

    public void setGroovyRules(String groovyRules) {
        this.groovyRules = groovyRules;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getConfigMainId() {
        return configMainId;
    }

    public void setConfigMainId(long configMainId) {
        this.configMainId = configMainId;
    }

    public double getGlobalTemperature() {
        return globalTemperature;
    }

    public void setGlobalTemperature(double globalTemperature) {
        this.globalTemperature = globalTemperature;
    }

    public String getSystemMsg() {
        return systemMsg;
    }

    public void setSystemMsg(String systemMsg) {
        this.systemMsg = systemMsg;
    }

    public List<String> getKnowledgeRepoIdList() {
        return knowledgeRepoIdList;
    }

    public void setKnowledgeRepoIdList(List<String> knowledgeRepoIdList) {
        this.knowledgeRepoIdList.clear();
        this.knowledgeRepoIdList.addAll(knowledgeRepoIdList);
    }

    public List<String> getAllowUsers() {
        return allowUsers;
    }

    public void setAllowUsers(List<String> allowUsers) {
        this.allowUsers.clear();
        this.allowUsers.addAll(allowUsers);
    }

    public List<ChatConfigDetail> getDetailList() {
        return detailList;
    }

    public void setDetailList(List<ChatConfigDetail> detailList) {
        this.detailList.clear();
        this.detailList.addAll(detailList);
    }
}
