package com.mkyuan.fountainbase.ai.bean;

import java.io.Serializable;

public class Message implements Serializable {

    private String role = "user";
    private String content = "";

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
