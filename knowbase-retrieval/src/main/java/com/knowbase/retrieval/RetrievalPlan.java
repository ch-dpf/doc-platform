package com.knowbase.retrieval;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record RetrievalPlan(
        List<UUID> libraryIds,
        Map<String, Object> retrievalPolicy,
        int topKPerLibrary,
        int maxCandidates,
        int maxEvidence,
        String fusion,
        String rerank
) {
}
