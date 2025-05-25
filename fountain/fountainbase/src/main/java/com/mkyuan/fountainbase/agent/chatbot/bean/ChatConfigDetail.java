package com.mkyuan.fountainbase.agent.chatbot.bean;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Document
public class ChatConfigDetail {

    @Id
    private String id;
    private long configMainId = 0l;
    private int sequence = 0;
    private String stepDescription = "";
    private int type = 1;//1-是否闲聊-2-降嗓去重主谓宾倒置并折成一个个,分隔的适用于搜索的关键字 3-BG25 HYDE GRM 4-把用户的会话打标签并作检索 5-rereank
    private boolean enabled = true;
    private String stepPrompt = "";
    private double temperature=0.1;
    private Date createdDate = new Date();
    private Date updatedDate = new Date();
    private String createdBy = "";
    private String userName="";
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public long getConfigMainId() {
        return configMainId;
    }

    public void setConfigMainId(long configMainId) {
        this.configMainId = configMainId;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public String getStepDescription() {
        return stepDescription;
    }

    public void setStepDescription(String stepDescription) {
        this.stepDescription = stepDescription;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getStepPrompt() {
        return stepPrompt;
    }

    public void setStepPrompt(String stepPrompt) {
        this.stepPrompt = stepPrompt;
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
}
