package com.knowbase.domain.model;

import com.knowbase.domain.status.DocumentStatus;

import java.time.Instant;
import java.util.UUID;

public record KnowledgeDocument(
        UUID documentId,
        UUID libraryId,
        UUID indexVersionId,
        String sourceUri,
        String title,
        DocumentStatus status,
        UUID documentProfileId,
        String contentHash,
        Instant lastIndexedAt,
        String lastError,
        Instant createdAt,
        Instant updatedAt
) {
}
