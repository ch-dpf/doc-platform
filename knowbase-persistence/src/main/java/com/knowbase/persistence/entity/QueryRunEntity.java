package com.knowbase.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowbase.persistence.handler.JsonbTypeHandler;

import java.time.Instant;
import java.util.UUID;

@TableName(value = "kb_query_run", autoResultMap = true)
public class QueryRunEntity {

    @TableId
    private UUID queryRunId;
    private UUID agentId;
    private UUID agentVersionId;
    private String status;
    private String question;
    private String answer;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String evidencePackJson;
    private String traceId;
    private Integer promptTokens;
    private Integer completionTokens;
    private Instant createdAt;
    private Instant completedAt;

    public UUID getQueryRunId() {
        return queryRunId;
    }

    public void setQueryRunId(UUID queryRunId) {
        this.queryRunId = queryRunId;
    }

    public UUID getAgentId() {
        return agentId;
    }

    public void setAgentId(UUID agentId) {
        this.agentId = agentId;
    }

    public UUID getAgentVersionId() {
        return agentVersionId;
    }

    public void setAgentVersionId(UUID agentVersionId) {
        this.agentVersionId = agentVersionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getEvidencePackJson() {
        return evidencePackJson;
    }

    public void setEvidencePackJson(String evidencePackJson) {
        this.evidencePackJson = evidencePackJson;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(Integer promptTokens) {
        this.promptTokens = promptTokens;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(Integer completionTokens) {
        this.completionTokens = completionTokens;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
