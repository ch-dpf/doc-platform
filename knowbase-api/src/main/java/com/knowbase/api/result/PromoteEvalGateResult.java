package com.knowbase.api.result;

import java.util.List;
import java.util.UUID;

public record PromoteEvalGateResult(
        UUID libraryId,
        boolean enabled,
        boolean passed,
        List<String> failures,
        List<String> messages,
        Double currentRecallAtK,
        Double baselineRecallAtK,
        Double recallThreshold,
        Double regressionDelta,
        Double regressionDeltaMax,
        UUID latestEvalRunId,
        UUID baselineEvalRunId
) {
    public PromoteEvalGateResult(
            UUID libraryId,
            boolean enabled,
            boolean passed,
            List<String> failures,
            List<String> messages
    ) {
        this(libraryId, enabled, passed, failures, messages, null, null, null, null, null, null, null);
    }

    public static PromoteEvalGateResult skipped(UUID libraryId) {
        return new PromoteEvalGateResult(libraryId, false, true, List.of(), List.of("promote 评测门禁未启用"));
    }
}
