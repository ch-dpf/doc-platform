package com.knowbase.api.result;

import java.util.List;
import java.util.UUID;

public record BatchReindexResult(
        UUID libraryId,
        int documentCount,
        List<String> sourceUris,
        IngestionRunResult ingestionRun
) {
}
