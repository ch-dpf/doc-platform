package com.docplatform.ingest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CollectRequest(
        @NotNull UUID libraryId,
        @NotBlank String tenantId,
        @NotBlank String url,
        boolean autoIndex
) {
}
