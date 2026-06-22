package com.knowbase.api.result;

import java.time.Instant;
import java.util.UUID;

public record ChatSessionResult(
        UUID sessionId,
        String tenantId,
        UUID agentId,
        UUID agentVersionId,
        String title,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
