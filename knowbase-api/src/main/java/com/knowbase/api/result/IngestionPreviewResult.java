package com.knowbase.api.result;

import java.util.List;
import java.util.UUID;

public record IngestionPreviewResult(
        UUID libraryId,
        int sourceCount,
        int succeededDocuments,
        int failedDocuments,
        int totalChunks,
        int indexableChunks,
        List<DocumentPreviewResult> documents
) {
}
