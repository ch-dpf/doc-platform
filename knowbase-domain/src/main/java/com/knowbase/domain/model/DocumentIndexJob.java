package com.knowbase.domain.model;

import java.time.Instant;
import java.util.UUID;

public record DocumentIndexJob(
        UUID jobId,
        UUID runId,
        UUID libraryId,
        UUID documentId,
        String sourceUri,
        String status,
        String stage,
        int chunkCount,
        String message,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {
}
