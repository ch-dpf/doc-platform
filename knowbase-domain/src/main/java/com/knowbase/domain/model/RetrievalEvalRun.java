package com.knowbase.domain.model;

import com.knowbase.domain.status.RetrievalEvalRunStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record RetrievalEvalRun(
        UUID evalRunId,
        UUID libraryId,
        RetrievalEvalRunStatus status,
        int hitK,
        int totalSamples,
        int passedSamples,
        Double recallAtK,
        Double mrr,
        Double contextPrecisionAtK,
        Map<String, Double> stratifiedRecall,
        Map<String, Object> retrievalPolicy,
        String message,
        Instant createdAt,
        Instant completedAt
) {
    public RetrievalEvalRun(
            UUID evalRunId,
            UUID libraryId,
            RetrievalEvalRunStatus status,
            int hitK,
            int totalSamples,
            int passedSamples,
            Double recallAtK,
            Map<String, Object> retrievalPolicy,
            String message,
            Instant createdAt,
            Instant completedAt
    ) {
        this(
                evalRunId,
                libraryId,
                status,
                hitK,
                totalSamples,
                passedSamples,
                recallAtK,
                null,
                null,
                Map.of(),
                retrievalPolicy,
                message,
                createdAt,
                completedAt
        );
    }
}
