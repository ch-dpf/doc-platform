package com.knowbase.ingestion;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record IngestionRequest(
        UUID runId,
        UUID libraryId,
        List<String> sourceUris,
        String documentProfileCode,
        boolean publishIndexOnSuccess,
        Map<String, Object> options
) {
}
