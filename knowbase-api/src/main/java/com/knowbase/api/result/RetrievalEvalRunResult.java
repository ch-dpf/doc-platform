package com.knowbase.api.result;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "库级批量召回评测运行")
public record RetrievalEvalRunResult(
        UUID evalRunId,
        UUID libraryId,
        String status,
        int hitK,
        int totalSamples,
        int passedSamples,
        Double recallAtK,
        Double mrr,
        Double contextPrecisionAtK,
        Map<String, Double> stratifiedRecall,
        Map<String, Object> retrievalPolicy,
        String message,
        List<RetrievalEvalResultItem> results,
        Instant createdAt,
        Instant completedAt
) {
    public RetrievalEvalRunResult(
            UUID evalRunId,
            UUID libraryId,
            String status,
            int hitK,
            int totalSamples,
            int passedSamples,
            Double recallAtK,
            Map<String, Object> retrievalPolicy,
            String message,
            List<RetrievalEvalResultItem> results,
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
                results,
                createdAt,
                completedAt
        );
    }
}
