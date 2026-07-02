package com.knowbase.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record RetrievalEvalResult(
        UUID resultId,
        UUID evalRunId,
        UUID sampleId,
        String question,
        boolean hit,
        int hitRankUsed,
        Integer firstHitRank,
        UUID matchedDocumentId,
        UUID matchedChunkId,
        String matchType,
        int retrievedCount,
        String failureReason,
        Map<String, Object> trace,
        Instant createdAt
) {
}
