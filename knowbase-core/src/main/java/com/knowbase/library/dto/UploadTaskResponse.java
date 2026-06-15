package com.knowbase.library.dto;

import com.knowbase.library.domain.UploadTask;
import com.knowbase.library.domain.UploadTaskStatus;

import java.time.Instant;
import java.util.UUID;

public record UploadTaskResponse(
        UUID taskId,
        UUID libraryId,
        String tenantId,
        String fileName,
        UploadTaskStatus status,
        int progress,
        UUID docId,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt) {

    public static UploadTaskResponse from(UploadTask task) {
        return new UploadTaskResponse(
                task.getTaskId(),
                task.getLibraryId(),
                task.getTenantId(),
                task.getFileName(),
                task.getStatus(),
                task.getProgress(),
                task.getDocId(),
                task.getErrorMessage(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }
}
