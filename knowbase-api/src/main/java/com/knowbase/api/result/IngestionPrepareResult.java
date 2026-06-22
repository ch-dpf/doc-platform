package com.knowbase.api.result;

import java.util.List;
import java.util.UUID;

public record IngestionPrepareResult(
        UUID libraryId,
        String prepareStage,
        int sourceCount,
        int succeeded,
        int failed,
        List<IngestionPrepareDocumentResult> documents
) {
}
