package com.knowbase.library.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowbase.platform.mybatis.PostgresJsonbTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.time.Instant;
import java.util.UUID;

@TableName("library_batch_job")
public class LibraryBatchJob {

    @TableId(value = "job_id", type = IdType.INPUT)
    private UUID jobId;

    @TableField("library_id")
    private UUID libraryId;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("job_type")
    private LibraryBatchJobType jobType;

    @TableField("chunk_profile_id")
    private String chunkProfileId;

    private LibraryBatchJobStatus status;

    @TableField("total_count")
    private int totalCount;

    @TableField("completed_count")
    private int completedCount;

    @TableField("failed_count")
    private int failedCount;

    @TableField("last_error")
    private String lastError;

    @TableField(value = "failed_doc_ids", jdbcType = JdbcType.OTHER, typeHandler = PostgresJsonbTypeHandler.class)
    private String failedDocIdsJson;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;

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

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public LibraryBatchJobType getJobType() {
        return jobType;
    }

    public void setJobType(LibraryBatchJobType jobType) {
        this.jobType = jobType;
    }

    public String getChunkProfileId() {
        return chunkProfileId;
    }

    public void setChunkProfileId(String chunkProfileId) {
        this.chunkProfileId = chunkProfileId;
    }

    public LibraryBatchJobStatus getStatus() {
        return status;
    }

    public void setStatus(LibraryBatchJobStatus status) {
        this.status = status;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(int completedCount) {
        this.completedCount = completedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public String getFailedDocIdsJson() {
        return failedDocIdsJson;
    }

    public void setFailedDocIdsJson(String failedDocIdsJson) {
        this.failedDocIdsJson = failedDocIdsJson;
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
