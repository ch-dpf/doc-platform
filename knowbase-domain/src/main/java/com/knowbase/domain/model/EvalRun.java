package com.knowbase.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record EvalRun(
        UUID evalRunId,
        String tenantId,
        UUID agentId,
        String evalType,
        String status,
        Map<String, Object> metrics,
        Instant createdAt,
        Instant finishedAt
) {
}
