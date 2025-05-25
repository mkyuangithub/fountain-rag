package com.mkyuan.fountainbase.agent.chatbot.bean;

public class RewritedQueryResult {
    private String rewritedPrompt="";
    private int chatType=1;

    public RewritedQueryResult() {
    }

    public RewritedQueryResult(String rewritedPrompt, int chatType) {
        this.rewritedPrompt = rewritedPrompt;
        this.chatType = chatType;
    }

    public String getRewritedPrompt() {
        return rewritedPrompt;
    }

    public void setRewritedPrompt(String rewritedPrompt) {
        this.rewritedPrompt = rewritedPrompt;
    }

    public int getChatType() {
        return chatType;
    }

    public void setChatType(int chatType) {
        this.chatType = chatType;
    }
}
