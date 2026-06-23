package com.knowbase.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

@Schema(description = "创建 Document Profile")
public record CreateDocumentProfileCommand(
        @NotBlank String code,
        @NotNull String contentFamily,
        @NotBlank String parserCode,
        @NotBlank String chunkingStrategy,
        UUID tokenizerProfileId,
        Map<String, Object> metadataSchema,
        Map<String, Object> options,
        Boolean enabled
) {
}
