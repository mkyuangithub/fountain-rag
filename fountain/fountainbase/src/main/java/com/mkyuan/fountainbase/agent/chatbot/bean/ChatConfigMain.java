package com.mkyuan.fountainbase.agent.chatbot.bean;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Document
public class ChatConfigMain {
    @Id
    private long id;
    private String systemMsg="";
    private String groovyRules="";
    private double temperature=0.1;
    private String userName="";
    private String description="";
    private Date createdDate=new Date();
    private Date updatedDate=new Date();
    private String createdBy="";
    private String rewriteSelectedDifySequenceNo="";
    private String chatSelectedDifySequenceNo="";

    public String getRewriteSelectedDifySequenceNo() {
        return rewriteSelectedDifySequenceNo;
    }

    public void setRewriteSelectedDifySequenceNo(String rewriteSelectedDifySequenceNo) {
        this.rewriteSelectedDifySequenceNo = rewriteSelectedDifySequenceNo;
    }

    public String getChatSelectedDifySequenceNo() {
        return chatSelectedDifySequenceNo;
    }

    public void setChatSelectedDifySequenceNo(String chatSelectedDifySequenceNo) {
        this.chatSelectedDifySequenceNo = chatSelectedDifySequenceNo;
    }

    private List<String> knowledgeRepoIdList=new ArrayList<>();
    private List<String> allowUsers=new ArrayList<>();

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getGroovyRules() {
        return groovyRules;
    }

    public void setGroovyRules(String groovyRules) {
        this.groovyRules = groovyRules;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getSystemMsg() {
        return systemMsg;
    }

    public void setSystemMsg(String systemMsg) {
        this.systemMsg = systemMsg;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(Date updatedDate) {
        this.updatedDate = updatedDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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
}
