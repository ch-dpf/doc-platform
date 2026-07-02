package com.knowbase.api.result;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record KnowledgeDocumentResult(
        UUID documentId,
        UUID libraryId,
        UUID indexVersionId,
        String sourceUri,
        String title,
        String status,
        int chunkCount,
        Instant lastIndexedAt,
        String lastError,
        Instant createdAt,
        Instant updatedAt
) {
}
