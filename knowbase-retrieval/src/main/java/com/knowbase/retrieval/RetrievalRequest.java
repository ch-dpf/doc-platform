package com.knowbase.retrieval;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record RetrievalRequest(
        UUID queryRunId,
        String question,
        List<UUID> libraryIds,
        Map<String, Object> retrievalPolicy
) {
}
