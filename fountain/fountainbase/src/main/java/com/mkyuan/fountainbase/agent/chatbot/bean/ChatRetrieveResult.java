package com.mkyuan.fountainbase.agent.chatbot.bean;

import com.mkyuan.fountainbase.vectordb.bean.query.QDRantQueryResult;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document
public class ChatRetrieveResult {
    @Id
    private String id;
    private String userName = "";
    private String modelName = "";
    private String originalPrompt = "";
    private String rewritedPrompt = "";
    private List<KnowledgeResult> retrieveResult = new ArrayList<>();
    private float averageScore = 0.0000f;

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

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getOriginalPrompt() {
        return originalPrompt;
    }

    public void setOriginalPrompt(String originalPrompt) {
        this.originalPrompt = originalPrompt;
    }

    public String getRewritedPrompt() {
        return rewritedPrompt;
    }

    public void setRewritedPrompt(String rewritedPrompt) {
        this.rewritedPrompt = rewritedPrompt;
    }

    public List<KnowledgeResult> getRetrieveResult() {
        return retrieveResult;
    }

    public void setRetrieveResult(List<KnowledgeResult> retrieveResult) {
        this.retrieveResult.clear();
        this.retrieveResult.addAll(retrieveResult);
    }

    public float getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(float averageScore) {
        this.averageScore = averageScore;
    }
}
