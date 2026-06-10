package com.knowbase.vector.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record RagRetrievalPreviewRequest(
        @NotNull UUID libraryId,
        @NotBlank String tenantId,
        @NotBlank String question,
        @Min(1) @Max(50) Integer topK,
        Double minScore,
        SearchRequest.SearchFilter filter,
        @Valid @Size(max = 40) List<RagChatMessage> history
) {}
