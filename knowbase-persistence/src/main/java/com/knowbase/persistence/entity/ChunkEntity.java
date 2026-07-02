package com.knowbase.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowbase.persistence.handler.JsonbTypeHandler;

import java.util.UUID;

@TableName(value = "kb_chunk", autoResultMap = true)
public class ChunkEntity {

    @TableId
    private UUID chunkId;
    private UUID documentId;
    private UUID libraryId;
    private UUID indexVersionId;
    private String content;
    private Integer tokenCount;
    private String tokenizerId;
    private String tokenizerVersion;
    private String embeddingModel;
    private String chunkBoundaryType;
    private UUID parentChunkId;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String metadataJson;

    public UUID getChunkId() {
        return chunkId;
    }

    public void setChunkId(UUID chunkId) {
        this.chunkId = chunkId;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public UUID getLibraryId() {
        return libraryId;
    }

    public void setLibraryId(UUID libraryId) {
        this.libraryId = libraryId;
    }

    public UUID getIndexVersionId() {
        return indexVersionId;
    }

    public void setIndexVersionId(UUID indexVersionId) {
        this.indexVersionId = indexVersionId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getTokenCount() {
        return tokenCount;
    }

    public void setTokenCount(Integer tokenCount) {
        this.tokenCount = tokenCount;
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

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public String getChunkBoundaryType() {
        return chunkBoundaryType;
    }

    public void setChunkBoundaryType(String chunkBoundaryType) {
        this.chunkBoundaryType = chunkBoundaryType;
    }

    public UUID getParentChunkId() {
        return parentChunkId;
    }

    public void setParentChunkId(UUID parentChunkId) {
        this.parentChunkId = parentChunkId;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }
}
