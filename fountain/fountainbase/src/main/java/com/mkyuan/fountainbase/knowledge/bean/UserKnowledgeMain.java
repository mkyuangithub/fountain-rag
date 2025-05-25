package com.mkyuan.fountainbase.knowledge.bean;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Document
public class UserKnowledgeMain {
    @Id
    private String id;
    private String userName;
    private String knowledgeName="";
    private String knowledgeRepoDescription="";
    private long knowledgeId=0l;
    private String majorPrompt="";
    private List<String> labelList=new ArrayList<>();
    private int fileCount=0;
    private long itemCount=0l;
    private long successCount=0l;
    private long failCount=0l;
    private int splitType=0;
    private String paragraphMark="\n";
    private int slideNums=1;
    private boolean vlEmbedding=false;

    public boolean isVlEmbedding() {
        return vlEmbedding;
    }

    public void setVlEmbedding(boolean vlEmbedding) {
        this.vlEmbedding = vlEmbedding;
    }

    public int getSplitType() {
        return splitType;
    }

    public void setSplitType(int splitType) {
        this.splitType = splitType;
    }

    public String getParagraphMark() {
        return paragraphMark;
    }

    public void setParagraphMark(String paragraphMark) {
        this.paragraphMark = paragraphMark;
    }

    public int getSlideNums() {
        return slideNums;
    }

    public void setSlideNums(int slideNums) {
        this.slideNums = slideNums;
    }

    private Date createdDate=new Date();
    private Date updatedDate=new Date();

    public int getFileCount() {
        return fileCount;
    }

    public void setFileCount(int fileCount) {
        this.fileCount = fileCount;
    }

    public long getItemCount() {
        return itemCount;
    }

    public void setItemCount(long itemCount) {
        this.itemCount = itemCount;
    }

    public long getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(long successCount) {
        this.successCount = successCount;
    }

    public long getFailCount() {
        return failCount;
    }

    public void setFailCount(long failCount) {
        this.failCount = failCount;
    }

    public String getMajorPrompt() {
        return majorPrompt;
    }

    public void setMajorPrompt(String majorPrompt) {
        this.majorPrompt = majorPrompt;
    }

    public List<String> getLabelList() {
        return labelList;
    }

    public void setLabelList(List<String> labelList) {
        this.labelList.clear();
        this.labelList.addAll(labelList);
    }

    public String getKnowledgeRepoDescription() {
        return knowledgeRepoDescription;
    }

    public void setKnowledgeRepoDescription(String knowledgeRepoDescription) {
        this.knowledgeRepoDescription = knowledgeRepoDescription;
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

    public String getKnowledgeName() {
        return knowledgeName;
    }

    public void setKnowledgeName(String knowledgeName) {
        this.knowledgeName = knowledgeName;
    }

    public long getKnowledgeId() {
        return knowledgeId;
    }

    public void setKnowledgeId(long knowledgeId) {
        this.knowledgeId = knowledgeId;
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
