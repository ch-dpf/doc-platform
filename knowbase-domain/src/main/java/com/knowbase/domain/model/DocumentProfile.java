package com.knowbase.domain.model;

import com.knowbase.domain.status.ContentFamily;

import java.util.Map;
import java.util.UUID;

public record DocumentProfile(
        UUID documentProfileId,
        UUID libraryId,
        String code,
        ContentFamily contentFamily,
        String parserCode,
        String chunkingStrategy,
        UUID tokenizerProfileId,
        Map<String, Object> metadataSchema,
        Map<String, Object> options,
        boolean enabled
) {
}
