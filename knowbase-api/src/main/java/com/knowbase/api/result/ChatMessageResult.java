package com.knowbase.api.result;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageResult(
        UUID messageId,
        UUID sessionId,
        String role,
        String content,
        UUID queryRunId,
        Instant createdAt
) {
}
