package com.knowbase.ingestion;

import java.util.Map;

public record StructuralSegment(
        String content,
        String boundaryType,
        int ordinal,
        Map<String, Object> metadata
) {
}
