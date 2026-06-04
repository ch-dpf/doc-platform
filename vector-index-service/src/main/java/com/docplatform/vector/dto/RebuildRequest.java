package com.docplatform.vector.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RebuildRequest(
        @NotNull UUID docId,
        @NotBlank String tenantId,
        int version,
        @NotBlank String parsedTextUrl
) {
}
