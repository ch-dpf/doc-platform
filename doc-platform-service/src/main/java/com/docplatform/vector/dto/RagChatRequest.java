package com.docplatform.vector.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RagChatRequest(
        @NotNull UUID libraryId,
        @NotBlank String tenantId,
        @NotBlank String question,
        @Min(1) @Max(20) Integer topK,
        Double minScore,
        SearchRequest.SearchFilter filter,
        /** 可选：覆盖全局 ollama.chat-model，仅本次问答 */
        String chatModel
) {
}
