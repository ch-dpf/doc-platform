package com.knowbase.vector.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ollama")
public class OllamaProperties {

    private String baseUrl = "http://localhost:11434";
    private String embeddingModel = "nomic-embed-text";
    private String chatModel = "llama3.2";
    private int timeoutSeconds = 60;
    private int chatTimeoutSeconds = 120;
    private int batchSize = 16;
    /** 对话采样温度，0 表示尽量确定性输出（企业问答推荐） */
    private double chatTemperature = 0.0;
    /** 固定随机种子，配合 temperature=0 提高同问同答一致性；null 表示不传 */
    private Integer chatSeed = 42;

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

    public double getChatTemperature() {
        return chatTemperature;
    }

    public void setChatTemperature(double chatTemperature) {
        this.chatTemperature = chatTemperature;
    }

    public Integer getChatSeed() {
        return chatSeed;
    }

    public void setChatSeed(Integer chatSeed) {
        this.chatSeed = chatSeed;
    }
}
