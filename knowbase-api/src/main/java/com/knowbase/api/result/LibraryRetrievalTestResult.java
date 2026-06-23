package com.knowbase.api.result;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record LibraryRetrievalTestResult(
        UUID retrievalTestId,
        UUID libraryId,
        String question,
        int candidateCount,
        List<EvidenceResult> evidence,
        List<CitationResult> citations,
        int contextTokens,
        String tokenizerId,
        String tokenizerVersion,
        boolean evidenceLow,
        Map<String, Object> trace,
        Instant createdAt,
        RetrievalHitCheckResult hitCheck
) {
    public LibraryRetrievalTestResult(
            UUID retrievalTestId,
            UUID libraryId,
            String question,
            int candidateCount,
            List<EvidenceResult> evidence,
            List<CitationResult> citations,
            int contextTokens,
            String tokenizerId,
            String tokenizerVersion,
            boolean evidenceLow,
            Map<String, Object> trace,
            Instant createdAt
    ) {
        this(
                retrievalTestId,
                libraryId,
                question,
                candidateCount,
                evidence,
                citations,
                contextTokens,
                tokenizerId,
                tokenizerVersion,
                evidenceLow,
                trace,
                createdAt,
                null
        );
    }
}
