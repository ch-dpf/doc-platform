package com.knowbase.api.result;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record EvalSampleResult(
        UUID sampleId,
        UUID evalRunId,
        String question,
        String expectedAnswer,
        String actualAnswer,
        Double score,
        Map<String, Object> metrics,
        Instant createdAt
) {
}
