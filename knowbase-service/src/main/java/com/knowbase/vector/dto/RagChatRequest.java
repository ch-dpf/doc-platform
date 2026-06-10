package com.knowbase.vector.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record RagChatRequest(
        @NotNull UUID libraryId,
        @NotBlank String tenantId,
        @NotBlank String question,
        @Min(1) @Max(20) Integer topK,
        Double minScore,
        SearchRequest.SearchFilter filter,
        /** 可选：覆盖全局 ollama.chat-model，仅本次问答 */
        String chatModel,
        /** 可选：多轮对话历史（不含当前 question），按时间顺序 */
        @Valid @Size(max = 40) List<RagChatMessage> history
) {
    public RagChatRequest(
            UUID libraryId,
            String tenantId,
            String question,
            Integer topK,
            Double minScore,
            SearchRequest.SearchFilter filter,
            String chatModel) {
        this(libraryId, tenantId, question, topK, minScore, filter, chatModel, null);
    }
}
