package com.knowbase.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RetrievalEvalSample(
        UUID sampleId,
        UUID libraryId,
        String question,
        List<UUID> expectedDocumentIds,
        List<String> expectedSourceUris,
        List<String> groundTruthContexts,
        int hitRank,
        String notes,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
