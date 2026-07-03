package com.knowbase.api.result;

public record IngestionQualityIssueResult(
        String stage,
        String severity,
        String title,
        String description,
        String action
) {
}
