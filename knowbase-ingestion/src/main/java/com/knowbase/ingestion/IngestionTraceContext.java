package com.knowbase.ingestion;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record IngestionTraceContext(
        UUID traceId,
        UUID runId,
        UUID documentId,
        String sourceUri
) {

    public Map<String, Object> attributes(Map<String, Object> extra) {
        Map<String, Object> attributes = new HashMap<>();
        if (traceId != null) {
            attributes.put("traceId", traceId);
        }
        if (runId != null) {
            attributes.put("runId", runId.toString());
        }
        if (documentId != null) {
            attributes.put("documentId", documentId.toString());
        }
        if (sourceUri != null && !sourceUri.isBlank()) {
            attributes.put("sourceUri", sourceUri);
        }
        if (extra != null) {
            attributes.putAll(extra);
        }
        return attributes;
    }
}
