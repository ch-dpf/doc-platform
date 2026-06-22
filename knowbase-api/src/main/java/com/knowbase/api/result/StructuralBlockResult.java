package com.knowbase.api.result;

import java.util.Map;

public record StructuralBlockResult(
        int ordinal,
        String blockType,
        int level,
        String contentPreview,
        Map<String, Object> metadata
) {
}
