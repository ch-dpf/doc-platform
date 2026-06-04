package com.docplatform.vector.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ollama")
public class OllamaProperties {

    private String baseUrl = "http://localhost:11434";
    private String embeddingModel = "nomic-embed-text";
    private String chatModel = "llama3.2";
    private int timeoutSeconds = 60;
    private int chatTimeoutSeconds = 120;
    private int batchSize = 16;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public String getChatModel() {
        return chatModel;
    }

    public void setChatModel(String chatModel) {
        this.chatModel = chatModel;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getChatTimeoutSeconds() {
        return chatTimeoutSeconds;
    }

    public void setChatTimeoutSeconds(int chatTimeoutSeconds) {
        this.chatTimeoutSeconds = chatTimeoutSeconds;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
}
