package com.docplatform.vector.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.Instant;
import java.util.UUID;

@TableName("document_index_job")
public class DocumentIndexJob {

    @TableId(value = "job_id", type = IdType.INPUT)
    private UUID jobId;

    @TableField("library_id")
    private UUID libraryId;

    @TableField("doc_id")
    private UUID docId;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("version")
    private int version;

    @TableField("status")
    private IndexJobStatus status;

    @TableField("error_message")
    private String errorMessage;

    @TableField("retry_count")
    private int retryCount;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("completed_at")
    private Instant completedAt;

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }

    public UUID getLibraryId() {
        return libraryId;
    }

    public void setLibraryId(UUID libraryId) {
        this.libraryId = libraryId;
    }

    public UUID getDocId() {
        return docId;
    }

    public void setDocId(UUID docId) {
        this.docId = docId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public IndexJobStatus getStatus() {
        return status;
    }

    public void setStatus(IndexJobStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
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
