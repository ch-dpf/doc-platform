package com.knowbase.api.result;

import java.util.Map;

public record ChunkPreviewResult(
        int ordinal,
        String content,
        int tokenCount,
        String boundaryType,
        boolean indexable,
        Map<String, Object> metadata
) {
}
