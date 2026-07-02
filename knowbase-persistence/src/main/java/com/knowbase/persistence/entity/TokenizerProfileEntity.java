package com.knowbase.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowbase.persistence.handler.JsonbTypeHandler;

import java.time.Instant;
import java.util.UUID;

@TableName("kb_tokenizer_profile")
public class TokenizerProfileEntity {

    @TableId
    private UUID tokenizerProfileId;
    private String provider;
    private String modelName;
    private String tokenizerId;
    private String tokenizerVersion;
    private Boolean approximate;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String configJson;
    private Boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;

    public UUID getTokenizerProfileId() {
        return tokenizerProfileId;
    }

    public void setTokenizerProfileId(UUID tokenizerProfileId) {
        this.tokenizerProfileId = tokenizerProfileId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getTokenizerId() {
        return tokenizerId;
    }

    public void setTokenizerId(String tokenizerId) {
        this.tokenizerId = tokenizerId;
    }

    public String getTokenizerVersion() {
        return tokenizerVersion;
    }

    public void setTokenizerVersion(String tokenizerVersion) {
        this.tokenizerVersion = tokenizerVersion;
    }

    public Boolean getApproximate() {
        return approximate;
    }

    public void setApproximate(Boolean approximate) {
        this.approximate = approximate;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
