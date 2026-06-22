package com.knowbase.ingestion;

import java.util.List;
import java.util.Map;

public record NormalizationResult(
        ParsedDocument document,
        int rawCharCount,
        int normalizedCharCount,
        int rawBlockCount,
        int normalizedBlockCount,
        List<String> appliedRules
) {

    public Map<String, Object> stats() {
        return Map.of(
                "rawCharCount", rawCharCount,
                "normalizedCharCount", normalizedCharCount,
                "rawBlockCount", rawBlockCount,
                "normalizedBlockCount", normalizedBlockCount,
                "appliedRules", List.copyOf(appliedRules)
        );
    }
}
