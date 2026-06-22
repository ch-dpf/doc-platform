package com.knowbase.domain.model;

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
        double score
) {
}
