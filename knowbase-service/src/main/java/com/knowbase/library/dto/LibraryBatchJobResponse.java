package com.knowbase.library.dto;

import com.knowbase.library.domain.LibraryBatchJob;
import com.knowbase.library.domain.LibraryBatchJobStatus;
import com.knowbase.library.domain.LibraryBatchJobType;
import com.knowbase.library.support.FailedDocIdsJson;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LibraryBatchJobResponse(
        UUID jobId,
        UUID libraryId,
        String tenantId,
        LibraryBatchJobType jobType,
        String chunkProfileId,
        LibraryBatchJobStatus status,
        int totalCount,
        int completedCount,
        int failedCount,
        int progressPercent,
        String lastError,
        List<UUID> failedDocIds,
        boolean retryable,
        Instant createdAt,
        Instant updatedAt) {

    public static LibraryBatchJobResponse from(LibraryBatchJob job) {
        int total = job.getTotalCount();
        int done = job.getCompletedCount() + job.getFailedCount();
        int percent = total <= 0 ? 0 : Math.min(100, done * 100 / total);
        List<UUID> failedDocIds = FailedDocIdsJson.parse(job.getFailedDocIdsJson());
        boolean retryable = isRetryable(job.getStatus(), failedDocIds);
        return new LibraryBatchJobResponse(
                job.getJobId(),
                job.getLibraryId(),
                job.getTenantId(),
                job.getJobType(),
                job.getChunkProfileId(),
                job.getStatus(),
                total,
                job.getCompletedCount(),
                job.getFailedCount(),
                percent,
                job.getLastError(),
                failedDocIds,
                retryable,
                job.getCreatedAt(),
                job.getUpdatedAt());
    }

    private static boolean isRetryable(LibraryBatchJobStatus status, List<UUID> failedDocIds) {
        return (status == LibraryBatchJobStatus.PARTIAL || status == LibraryBatchJobStatus.FAILED)
                && failedDocIds != null
                && !failedDocIds.isEmpty();
    }
}
