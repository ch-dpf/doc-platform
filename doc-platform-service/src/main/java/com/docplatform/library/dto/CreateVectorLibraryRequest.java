package com.docplatform.library.dto;

import com.docplatform.library.config.VectorLibraryConfig;
import jakarta.validation.constraints.NotBlank;

public record CreateVectorLibraryRequest(
        @NotBlank String tenantId,
        @NotBlank String name,
        String description,
        VectorLibraryConfig config) {}
