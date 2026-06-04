package com.docplatform.ingest.dto;

import jakarta.validation.constraints.NotBlank;

public record CollectRequest(
        @NotBlank String tenantId,
        @NotBlank String url,
        boolean autoIndex
) {
}
