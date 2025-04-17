package com.bervan.shstat.queue;

import java.io.Serializable;

public class QueueMessage implements Serializable {
    private String supportClassName;
    private Serializable body;
    private String apiKey;

    public QueueMessage(String supportClassName, Serializable body, String apiKey) {
        this.supportClassName = supportClassName;
        this.body = body;
        this.apiKey = apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiKey() {
        return apiKey;
    }


    public void setBody(Serializable body) {
        this.body = body;
    }


    public Serializable getBody() {
        return body;
    }

    public void setSupportClassName(String supportClassName) {
        this.supportClassName = supportClassName;
    }

    public String getSupportClassName() {
        return supportClassName;
    }
}