package com.knowbase.domain.model;

import java.time.Instant;
import java.util.UUID;

public record RetrievalEvalBaseline(
        UUID libraryId,
        UUID evalRunId,
        UUID profileId,
        UUID indexGenerationId,
        double recallAtK,
        int hitK,
        Instant createdAt,
        Instant updatedAt
) {
}
