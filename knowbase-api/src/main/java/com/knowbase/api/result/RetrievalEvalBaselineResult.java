package com.knowbase.api.result;

import java.time.Instant;
import java.util.UUID;

public record RetrievalEvalBaselineResult(
        UUID libraryId,
        UUID evalRunId,
        UUID profileId,
        UUID indexGenerationId,
        double recallAtK,
        int hitK,
        Instant recordedAt
) {
}
