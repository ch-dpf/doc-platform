package com.knowbase.domain.model;

import java.util.Map;
import java.util.UUID;

public record EvidenceSegment(
        UUID evidenceId,
        UUID libraryId,
        UUID documentId,
        UUID chunkId,
        UUID indexVersionId,
        String content,
        double score,
        Map<String, Object> metadata
) {
}
