package com.mkyuan.fountainbase.agent.chatbot.bean;

import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document
public class ChatConfigBackup {
    private String id;
    private String userName="";
    private String backupDescr="";
    private String fileCode="";
    private Date createdDate=new Date();
    private Date updatedDate=new Date();

    public String getBackupDescr() {
        return backupDescr;
    }

    public void setBackupDescr(String backupDescr) {
        this.backupDescr = backupDescr;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getFileCode() {
        return fileCode;
    }

    public void setFileCode(String fileCode) {
        this.fileCode = fileCode;
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
}
