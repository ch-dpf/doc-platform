package com.knowbase.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowbase.persistence.handler.JsonbTypeHandler;

import java.util.UUID;

@TableName(value = "kb_document_profile", autoResultMap = true)
public class DocumentProfileEntity {

    @TableId
    private UUID documentProfileId;
    private UUID libraryId;
    private String code;
    private String contentFamily;
    private String parserCode;
    private String chunkingStrategy;
    private UUID tokenizerProfileId;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String metadataSchema;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String optionsJson;
    private Boolean enabled;

    public UUID getDocumentProfileId() {
        return documentProfileId;
    }

    public void setDocumentProfileId(UUID documentProfileId) {
        this.documentProfileId = documentProfileId;
    }

    public UUID getLibraryId() {
        return libraryId;
    }

    public void setLibraryId(UUID libraryId) {
        this.libraryId = libraryId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getContentFamily() {
        return contentFamily;
    }

    public void setContentFamily(String contentFamily) {
        this.contentFamily = contentFamily;
    }

    public String getParserCode() {
        return parserCode;
    }

    public void setParserCode(String parserCode) {
        this.parserCode = parserCode;
    }

    public String getChunkingStrategy() {
        return chunkingStrategy;
    }

    public void setChunkingStrategy(String chunkingStrategy) {
        this.chunkingStrategy = chunkingStrategy;
    }

    public UUID getTokenizerProfileId() {
        return tokenizerProfileId;
    }

    public void setTokenizerProfileId(UUID tokenizerProfileId) {
        this.tokenizerProfileId = tokenizerProfileId;
    }

    public String getMetadataSchema() {
        return metadataSchema;
    }

    public void setMetadataSchema(String metadataSchema) {
        this.metadataSchema = metadataSchema;
    }

    public String getOptionsJson() {
        return optionsJson;
    }

    public void setOptionsJson(String optionsJson) {
        this.optionsJson = optionsJson;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
