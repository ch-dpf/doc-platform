package com.knowbase.api.result;

import java.util.List;
import java.util.Map;

public record NormalizeStageResult(
        int rawCharCount,
        int normalizedCharCount,
        int rawBlockCount,
        int normalizedBlockCount,
        List<String> appliedRules,
        String textPreview,
        Map<String, Object> stats
) {
}
