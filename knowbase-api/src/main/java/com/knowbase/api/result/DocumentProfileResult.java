package com.knowbase.api.result;

import java.util.Map;
import java.util.UUID;

public record DocumentProfileResult(
        UUID documentProfileId,
        UUID libraryId,
        String code,
        String contentFamily,
        String parserCode,
        String chunkingStrategy,
        UUID tokenizerProfileId,
        Map<String, Object> metadataSchema,
        Map<String, Object> options,
        boolean enabled
) {
}
