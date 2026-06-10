package com.knowbase.vector.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RebuildLibraryRequest(
        @NotNull UUID libraryId,
        @NotBlank String tenantId
) {
}
