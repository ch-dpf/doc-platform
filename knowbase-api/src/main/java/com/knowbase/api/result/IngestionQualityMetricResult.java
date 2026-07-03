package com.knowbase.api.result;

public record IngestionQualityMetricResult(
        String key,
        String label,
        String value,
        String status,
        String description
) {
}
