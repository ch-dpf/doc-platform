package com.knowbase.domain.model;

import java.time.Instant;
import java.util.UUID;

public record IngestionDocumentError(
        UUID errorId,
        UUID runId,
        String sourceUri,
        String errorCode,
        String errorMessage,
        Instant createdAt
) {
}
