package com.knowbase.tokenizer;

import java.util.Map;

public record TokenChunk(
        String content,
        int tokenCount,
        int ordinal,
        String boundaryType,
        Map<String, Object> metadata
) {
}
