package com.knowbase.library.dto;

import jakarta.validation.constraints.NotBlank;

public record ArchiveChunkProfileRequest(
        @NotBlank String tenantId,
        @NotBlank String chunkProfileId) {}
