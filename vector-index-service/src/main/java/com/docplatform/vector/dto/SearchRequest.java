package com.docplatform.vector.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public record SearchRequest(
        @NotBlank String tenantId,
        @NotBlank String query,
        @Min(1) @Max(50) int topK,
        SearchFilter filter
) {
    public record SearchFilter(List<UUID> docIds) {
    }
}
