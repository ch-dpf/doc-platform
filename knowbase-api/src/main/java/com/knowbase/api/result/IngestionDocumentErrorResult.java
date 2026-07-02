package com.knowbase.api.result;

import java.time.Instant;
import java.util.UUID;

public record IngestionDocumentErrorResult(
        UUID errorId,
        UUID runId,
        String sourceUri,
        String errorCode,
        String errorMessage,
        Instant createdAt
) {
}
