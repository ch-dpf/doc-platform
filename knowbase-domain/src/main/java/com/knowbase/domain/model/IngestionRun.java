package com.knowbase.domain.model;

import com.knowbase.domain.status.IngestionRunStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record IngestionRun(
        UUID runId,
        UUID libraryId,
        IngestionRunStatus status,
        List<String> sourceUris,
        String sourceType,
        String documentProfileCode,
        boolean publishIndexOnSuccess,
        int inputDocuments,
        int succeededDocuments,
        int failedDocuments,
        int chunkCount,
        UUID indexVersionId,
        String message,
        Map<String, Object> options,
        Instant createdAt,
        Instant updatedAt
) {
}
