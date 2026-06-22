package com.knowbase.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowbase.persistence.handler.JsonbTypeHandler;

import java.time.Instant;
import java.util.UUID;

@TableName(value = "kb_ingestion_run", autoResultMap = true)
public class IngestionRunEntity {

    @TableId
    private UUID runId;
    private UUID libraryId;
    private String status;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String sourceUris;
    private String sourceType;
    private String documentProfileCode;
    private Boolean publishIndexOnSuccess;
    private Integer inputDocuments;
    private Integer succeededDocuments;
    private Integer failedDocuments;
    private Integer chunkCount;
    private UUID indexVersionId;
    private String message;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String optionsJson;
    private Instant createdAt;
    private Instant updatedAt;

    public UUID getRunId() {
        return runId;
    }

    public void setRunId(UUID runId) {
        this.runId = runId;
    }

    public UUID getLibraryId() {
        return libraryId;
    }

    public void setLibraryId(UUID libraryId) {
        this.libraryId = libraryId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSourceUris() {
        return sourceUris;
    }

    public void setSourceUris(String sourceUris) {
        this.sourceUris = sourceUris;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getDocumentProfileCode() {
        return documentProfileCode;
    }

    public void setDocumentProfileCode(String documentProfileCode) {
        this.documentProfileCode = documentProfileCode;
    }

    public Boolean getPublishIndexOnSuccess() {
        return publishIndexOnSuccess;
    }

    public void setPublishIndexOnSuccess(Boolean publishIndexOnSuccess) {
        this.publishIndexOnSuccess = publishIndexOnSuccess;
    }

    public Integer getInputDocuments() {
        return inputDocuments;
    }

    public void setInputDocuments(Integer inputDocuments) {
        this.inputDocuments = inputDocuments;
    }

    public Integer getSucceededDocuments() {
        return succeededDocuments;
    }

    public void setSucceededDocuments(Integer succeededDocuments) {
        this.succeededDocuments = succeededDocuments;
    }

    public Integer getFailedDocuments() {
        return failedDocuments;
    }

    public void setFailedDocuments(Integer failedDocuments) {
        this.failedDocuments = failedDocuments;
    }

    public Integer getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(Integer chunkCount) {
        this.chunkCount = chunkCount;
    }

    public UUID getIndexVersionId() {
        return indexVersionId;
    }

    public void setIndexVersionId(UUID indexVersionId) {
        this.indexVersionId = indexVersionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getOptionsJson() {
        return optionsJson;
    }

    public void setOptionsJson(String optionsJson) {
        this.optionsJson = optionsJson;
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
