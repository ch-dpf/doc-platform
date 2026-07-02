package com.knowbase.retrieval;

import java.util.Map;

public final class ChunkRetrievalSupport {

    private ChunkRetrievalSupport() {
    }

    public static boolean isRetrievalEnabled(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return true;
        }
        Object flag = metadata.get("retrievalEnabled");
        if (flag == null) {
            return true;
        }
        if (flag instanceof Boolean enabled) {
            return enabled;
        }
        return !"false".equalsIgnoreCase(String.valueOf(flag));
    }
}
