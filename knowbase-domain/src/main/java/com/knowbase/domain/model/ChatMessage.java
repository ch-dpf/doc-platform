package com.knowbase.domain.model;

import java.time.Instant;
import java.util.UUID;

public record ChatMessage(
        UUID messageId,
        UUID sessionId,
        String role,
        String content,
        UUID queryRunId,
        Instant createdAt
) {
}
