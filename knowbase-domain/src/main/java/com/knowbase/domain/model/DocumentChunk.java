package com.knowbase.domain.model;

import java.util.Map;
import java.util.UUID;

public record DocumentChunk(
        UUID chunkId,
        UUID documentId,
        UUID libraryId,
        UUID indexVersionId,
        String content,
        int tokenCount,
        String tokenizerId,
        String tokenizerVersion,
        String embeddingModel,
        String chunkBoundaryType,
        UUID parentChunkId,
        Map<String, Object> metadata
) {
}
