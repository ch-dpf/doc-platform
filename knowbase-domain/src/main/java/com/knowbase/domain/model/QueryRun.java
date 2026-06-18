package com.knowbase.domain.model;

import com.knowbase.domain.status.QueryRunStatus;

import java.time.Instant;
import java.util.UUID;

public record QueryRun(
        UUID queryRunId,
        UUID agentId,
        UUID agentVersionId,
        QueryRunStatus status,
        String question,
        String answer,
        EvidencePack evidencePack,
        String traceId,
        int promptTokens,
        int completionTokens,
        Instant createdAt,
        Instant completedAt
) {
}
