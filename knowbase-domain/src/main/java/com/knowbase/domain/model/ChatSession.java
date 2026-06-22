package com.knowbase.domain.model;

import java.time.Instant;
import java.util.UUID;

public record ChatSession(
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
