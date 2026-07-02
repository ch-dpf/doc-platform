package com.knowbase.retrieval;

import java.util.HashMap;
import java.util.Map;

public final class RetrievalMetadata {

    private RetrievalMetadata() {
    }

    public static Map<String, Object> enrich(
            Map<String, Object> metadata,
            double vectorScore,
            double keywordScore,
            String retrievalMode
    ) {
        Map<String, Object> enriched = new HashMap<>();
        if (metadata != null) {
            enriched.putAll(metadata);
        }
        enriched.put("vectorScore", vectorScore);
        enriched.put("keywordScore", keywordScore);
        enriched.put("retrievalMode", retrievalMode);
        return Map.copyOf(enriched);
    }
}
