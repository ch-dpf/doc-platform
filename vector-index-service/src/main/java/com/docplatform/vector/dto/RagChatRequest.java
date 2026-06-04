package com.docplatform.vector.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record RagChatRequest(
        @NotBlank String tenantId,
        @NotBlank String question,
        @Min(1) @Max(20) Integer topK,
        Double minScore,
        SearchRequest.SearchFilter filter
) {
}
