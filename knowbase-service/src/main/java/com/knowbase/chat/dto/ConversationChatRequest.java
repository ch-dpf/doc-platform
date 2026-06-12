package com.knowbase.chat.dto;

import com.knowbase.vector.dto.SearchRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ConversationChatRequest(
        @NotBlank String tenantId,
        @NotBlank String question,
        @Min(1) @Max(20) Integer topK,
        Double minScore,
        SearchRequest.SearchFilter filter,
        String chatModel,
        Boolean includeAllChunkProfiles,
        List<String> chunkProfileIds) {}
