package com.knowbase.chat.dto;

import java.time.Instant;
import java.util.UUID;

public record ConversationResponse(
        UUID conversationId,
        UUID libraryId,
        String tenantId,
        String title,
        String summary,
        int messageCount,
        Instant createdAt,
        Instant updatedAt
) {}
