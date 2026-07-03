package com.knowbase.api.result;

import java.util.List;
import java.util.Map;

public record IngestionQualityInsightResult(
        String level,
        int score,
        String summary,
        List<IngestionQualityMetricResult> metrics,
        List<IngestionQualityIssueResult> issues,
        List<String> recommendedActions,
        Map<String, Object> facts
) {
}
