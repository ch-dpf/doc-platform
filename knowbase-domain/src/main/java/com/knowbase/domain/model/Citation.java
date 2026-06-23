package com.knowbase.domain.model;

import java.util.Map;
import java.util.UUID;

public record Citation(
        UUID citationId,
        UUID libraryId,
        UUID documentId,
        UUID chunkId,
        UUID indexVersionId,
        String sourceTitle,
        String sourceUri,
        String snippet,
        double score,
        Map<String, Object> metadata
) {
    public Citation(
            UUID citationId,
            UUID libraryId,
            UUID documentId,
            UUID chunkId,
            UUID indexVersionId,
            String sourceTitle,
            String sourceUri,
            String snippet,
            double score
    ) {
        this(citationId, libraryId, documentId, chunkId, indexVersionId, sourceTitle, sourceUri, snippet, score, Map.of());
    }
}
