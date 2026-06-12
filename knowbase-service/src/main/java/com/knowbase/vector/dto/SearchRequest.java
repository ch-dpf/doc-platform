package com.knowbase.vector.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SearchRequest(
        @NotNull UUID libraryId,
        @NotBlank String tenantId,
        @NotBlank String query,
        @Min(1) @Max(50) int topK,
        SearchFilter filter,
        Boolean includeAllChunkProfiles,
        List<String> chunkProfileIds) {

    public SearchRequest(UUID libraryId, String tenantId, String query, int topK, SearchFilter filter) {
        this(libraryId, tenantId, query, topK, filter, false, null);
    }

    public record SearchFilter(List<UUID> docIds, Map<String, String> metadata) {
        public SearchFilter(List<UUID> docIds) {
            this(docIds, null);
        }
    }
}
