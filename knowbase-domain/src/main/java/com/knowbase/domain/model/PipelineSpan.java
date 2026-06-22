package com.knowbase.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PipelineSpan(
        UUID spanId,
        UUID traceId,
        String pipeline,
        UUID runId,
        String stage,
        String status,
        Long durationMs,
        Map<String, Object> attributes,
        Instant startedAt,
        Instant finishedAt
) {
}
