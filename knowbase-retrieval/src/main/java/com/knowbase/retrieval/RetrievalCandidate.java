package com.knowbase.retrieval;

import java.util.Map;
import java.util.UUID;

public record RetrievalCandidate(
        UUID libraryId,
        UUID documentId,
        UUID chunkId,
        UUID indexVersionId,
        String content,
        double score,
        Map<String, Object> metadata
) {
}
