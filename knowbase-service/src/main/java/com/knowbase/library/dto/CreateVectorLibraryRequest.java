package com.knowbase.library.dto;

import com.knowbase.library.config.VectorLibraryConfig;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateVectorLibraryRequest(
        @NotBlank String tenantId,
        @NotBlank String name,
        String description,
        List<String> tags,
        VectorLibraryConfig config) {}
