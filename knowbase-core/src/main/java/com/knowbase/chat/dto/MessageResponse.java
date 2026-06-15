package com.knowbase.chat.dto;

import com.knowbase.vector.dto.RagCitation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MessageResponse(
        UUID messageId,
        String role,
        String content,
        List<RagCitation> citations,
        String searchQuery,
        Instant createdAt
) {}
