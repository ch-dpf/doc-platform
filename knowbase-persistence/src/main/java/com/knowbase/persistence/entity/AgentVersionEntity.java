package com.knowbase.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowbase.persistence.handler.JsonbTypeHandler;

import java.time.Instant;
import java.util.UUID;

@TableName(value = "kb_agent_version", autoResultMap = true)
public class AgentVersionEntity {

    @TableId
    private UUID agentVersionId;
    private UUID agentId;
    private Integer version;
    private String status;
    private String scenePresetCode;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String libraryIds;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String routingPolicyJson;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String retrievalPolicyJson;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String answerPolicyJson;
    private String systemPrompt;
    private UUID chatTokenizerProfileId;
    private Boolean published;
    private Instant createdAt;

    public UUID getAgentVersionId() {
        return agentVersionId;
    }

    public void setAgentVersionId(UUID agentVersionId) {
        this.agentVersionId = agentVersionId;
    }

    public UUID getAgentId() {
        return agentId;
    }

    public void setAgentId(UUID agentId) {
        this.agentId = agentId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getScenePresetCode() {
        return scenePresetCode;
    }

    public void setScenePresetCode(String scenePresetCode) {
        this.scenePresetCode = scenePresetCode;
    }

    public String getLibraryIds() {
        return libraryIds;
    }

    public void setLibraryIds(String libraryIds) {
        this.libraryIds = libraryIds;
    }

    public String getRoutingPolicyJson() {
        return routingPolicyJson;
    }

    public void setRoutingPolicyJson(String routingPolicyJson) {
        this.routingPolicyJson = routingPolicyJson;
    }

    public String getRetrievalPolicyJson() {
        return retrievalPolicyJson;
    }

    public void setRetrievalPolicyJson(String retrievalPolicyJson) {
        this.retrievalPolicyJson = retrievalPolicyJson;
    }

    public String getAnswerPolicyJson() {
        return answerPolicyJson;
    }

    public void setAnswerPolicyJson(String answerPolicyJson) {
        this.answerPolicyJson = answerPolicyJson;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public UUID getChatTokenizerProfileId() {
        return chatTokenizerProfileId;
    }

    public void setChatTokenizerProfileId(UUID chatTokenizerProfileId) {
        this.chatTokenizerProfileId = chatTokenizerProfileId;
    }

    public Boolean getPublished() {
        return published;
    }

    public void setPublished(Boolean published) {
        this.published = published;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
