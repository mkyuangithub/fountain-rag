package com.mkyuan.fountainbase.ai.bean;

import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class AIFunctionals {
    private String id;
    private int code=0;
    private int type=1;
    private String prompt="";
    private String description="";
    private String returnTemplate="";

    public String getReturnTemplate() {
        return returnTemplate;
    }

    public void setReturnTemplate(String returnTemplate) {
        this.returnTemplate = returnTemplate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}
