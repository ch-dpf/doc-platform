package com.knowbase.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowbase.persistence.handler.JsonbTypeHandler;

import java.time.Instant;
import java.util.UUID;

@TableName(value = "kb_library", autoResultMap = true)
public class LibraryEntity {

    @TableId
    private UUID libraryId;
    private String tenantId;
    private String name;
    private String description;
    private String status;
    private String libraryTypePresetCode;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String tags;
    private Instant createdAt;
    private Instant updatedAt;

    public UUID getLibraryId() {
        return libraryId;
    }

    public void setLibraryId(UUID libraryId) {
        this.libraryId = libraryId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLibraryTypePresetCode() {
        return libraryTypePresetCode;
    }

    public void setLibraryTypePresetCode(String libraryTypePresetCode) {
        this.libraryTypePresetCode = libraryTypePresetCode;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
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
