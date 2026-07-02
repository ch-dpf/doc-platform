package com.knowbase.api.command;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;
import java.util.UUID;

@Schema(description = "更新 Document Profile")
public record UpdateDocumentProfileCommand(
        String contentFamily,
        String parserCode,
        String chunkingStrategy,
        UUID tokenizerProfileId,
        Map<String, Object> metadataSchema,
        Map<String, Object> options,
        Boolean enabled
) {
}
