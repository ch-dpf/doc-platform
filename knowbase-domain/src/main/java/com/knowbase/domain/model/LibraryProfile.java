package com.knowbase.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record LibraryProfile(
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
        Instant createdAt
) {
}
