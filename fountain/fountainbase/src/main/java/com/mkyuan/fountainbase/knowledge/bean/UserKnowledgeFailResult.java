package com.mkyuan.fountainbase.knowledge.bean;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Document
public class UserKnowledgeFailResult {

    @Id
    private String id;
    private String userName="";
    private String knowledgeRepoId="";//哪个库的训练记录指向那个库的UserKnowledgeMain里的id
    private long fileId=0l;//
    private String fileChunkContent=""; //己切片好的内容
    private boolean handled=false;
    private String originalFileName="";
    private String contentMd5="";
    private List<String> labels=new ArrayList<>();
    private Date updatedDate=new Date();
    private Date createdDate=new Date();

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels.clear();
        this.labels.addAll(labels);
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKnowledgeRepoId() {
        return knowledgeRepoId;
    }

    public void setKnowledgeRepoId(String knowledgeRepoId) {
        this.knowledgeRepoId = knowledgeRepoId;
    }

    public long getFileId() {
        return fileId;
    }

    public void setFileId(long fileId) {
        this.fileId = fileId;
    }

    public String getFileChunkContent() {
        return fileChunkContent;
    }

    public void setFileChunkContent(String fileChunkContent) {
        this.fileChunkContent = fileChunkContent;
    }

    public boolean isHandled() {
        return handled;
    }

    public void setHandled(boolean handled) {
        this.handled = handled;
    }

    public String getContentMd5() {
        return contentMd5;
    }

    public void setContentMd5(String contentMd5) {
        this.contentMd5 = contentMd5;
    }

    public Date getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(Date updatedDate) {
        this.updatedDate = updatedDate;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }
}
