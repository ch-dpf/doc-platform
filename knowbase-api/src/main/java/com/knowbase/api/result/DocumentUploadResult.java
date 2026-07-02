package com.knowbase.api.result;

import java.util.List;

public record DocumentUploadResult(
        BatchObjectUploadResult upload,
        IngestionRunResult ingestionRun,
        List<KnowledgeDocumentResult> documents
) {
}
