package com.knowbase.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowbase.persistence.handler.JsonbTypeHandler;

import java.time.Instant;
import java.util.UUID;

@TableName(value = "kb_library_profile", autoResultMap = true)
public class LibraryProfileEntity {

    @TableId
    private UUID profileId;
    private UUID libraryId;
    private Integer version;
    private String embeddingProvider;
    private String embeddingModel;
    private Integer embeddingDimension;
    private UUID embeddingTokenizerProfileId;
    private Integer chunkMaxTokens;
    private Integer chunkOverlapTokens;
    private Integer retrievalTopK;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String optionsJson;
    private Instant createdAt;

    public UUID getProfileId() {
        return profileId;
    }

    public void setProfileId(UUID profileId) {
        this.profileId = profileId;
    }

    public UUID getLibraryId() {
        return libraryId;
    }

    public void setLibraryId(UUID libraryId) {
        this.libraryId = libraryId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getEmbeddingProvider() {
        return embeddingProvider;
    }

    public void setEmbeddingProvider(String embeddingProvider) {
        this.embeddingProvider = embeddingProvider;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public Integer getEmbeddingDimension() {
        return embeddingDimension;
    }

    public void setEmbeddingDimension(Integer embeddingDimension) {
        this.embeddingDimension = embeddingDimension;
    }

    public UUID getEmbeddingTokenizerProfileId() {
        return embeddingTokenizerProfileId;
    }

    public void setEmbeddingTokenizerProfileId(UUID embeddingTokenizerProfileId) {
        this.embeddingTokenizerProfileId = embeddingTokenizerProfileId;
    }

    public Integer getChunkMaxTokens() {
        return chunkMaxTokens;
    }

    public void setChunkMaxTokens(Integer chunkMaxTokens) {
        this.chunkMaxTokens = chunkMaxTokens;
    }

    public Integer getChunkOverlapTokens() {
        return chunkOverlapTokens;
    }

    public void setChunkOverlapTokens(Integer chunkOverlapTokens) {
        this.chunkOverlapTokens = chunkOverlapTokens;
    }

    public Integer getRetrievalTopK() {
        return retrievalTopK;
    }

    public void setRetrievalTopK(Integer retrievalTopK) {
        this.retrievalTopK = retrievalTopK;
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
}
