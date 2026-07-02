package com.knowbase.ingestion.table;

import java.util.List;

public record TableParseConfidence(
        double score,
        List<String> reasons,
        int coordinateFallbackRows,
        int dataRows,
        int headerRows,
        int layoutRows,
        int tableRegionCount
) {
    public TableParseConfidence {
        score = Math.max(0d, Math.min(1d, score));
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }
}
