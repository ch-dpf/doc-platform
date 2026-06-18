package com.knowbase.domain.model;

import com.knowbase.domain.status.AgentStatus;

import java.time.Instant;
import java.util.UUID;

public record KnowledgeAgent(
        UUID agentId,
        String tenantId,
        String name,
        String description,
        AgentStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
