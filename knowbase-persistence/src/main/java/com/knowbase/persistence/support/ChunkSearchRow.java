package com.knowbase.persistence.support;

import java.util.Map;
import java.util.UUID;

public record ChunkSearchRow(
        UUID chunkId,
        UUID documentId,
        UUID libraryId,
        UUID indexVersionId,
        String content,
        String metadataJson,
        double score
) {
    public Map<String, Object> metadata() {
        return JsonSupport.readMap(metadataJson);
    }
}
