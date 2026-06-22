package com.knowbase.api.result;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record EvalRunResult(
        UUID evalRunId,
        String tenantId,
        UUID agentId,
        String evalType,
        String status,
        Map<String, Object> metrics,
        List<EvalSampleResult> samples,
        Instant createdAt,
        Instant finishedAt
) {
}
