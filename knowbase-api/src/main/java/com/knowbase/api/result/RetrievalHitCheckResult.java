package com.knowbase.api.result;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "单题召回 Hit@K 判定结果")
public record RetrievalHitCheckResult(
        boolean hit,
        int hitRankUsed,
        Integer firstHitRank,
        UUID matchedDocumentId,
        UUID matchedChunkId,
        String matchType,
        int retrievedCount,
        String failureReason
) {
}
