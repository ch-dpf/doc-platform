package com.knowbase.api.result;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Schema(description = "单条样本的召回评测结果")
public record RetrievalEvalResultItem(
        UUID resultId,
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
