package com.knowbase.api.result;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record LibraryProfileResult(
        UUID profileId,
        UUID libraryId,
        int version,
        String embeddingProvider,
        String embeddingModel,
        int embeddingDimension,
        UUID embeddingTokenizerProfileId,
        int chunkMaxTokens,
        int chunkOverlapTokens,
        int retrievalTopK,
        Map<String, Object> options,
        Instant createdAt,
        UUID activeGenerationProfileId,
        boolean l1DriftDetected,
        List<String> driftFields,
        String driftMessage
) {
}
