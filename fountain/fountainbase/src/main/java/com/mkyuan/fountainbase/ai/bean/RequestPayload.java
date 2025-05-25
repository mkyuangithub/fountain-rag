package com.mkyuan.fountainbase.ai.bean;

import java.util.ArrayList;
import java.util.List;

public class RequestPayload {
    private double top_p=0.95;
    private boolean stream=false;
    private double temperature=0.1;
    private List<Object> messages = new ArrayList<>(); // Can hold both SystemMessage and UserMessage
    private int max_tokens=3172;
    private String model;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public double getTop_p() {
        return top_p;
    }

    public void setTop_p(double top_p) {
        this.top_p = top_p;
    }

    public int getMax_tokens() {
        return max_tokens;
    }

    public void setMax_tokens(int max_tokens) {
        this.max_tokens = max_tokens;
    }
    // Getters and Setters
    // ...


    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public List<Object> getMessages() {
        return messages;
    }

    public void setMessages(List<Object> messages) {
        //this.messages = messages;
        this.messages.clear();
        this.messages.addAll(messages);
    }
}
