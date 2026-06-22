package com.knowbase.api.result;

public record IngestionPrepareDocumentResult(
        String sourceUri,
        String title,
        String documentProfileCode,
        String contentFamily,
        String prepareStage,
        ParseStageResult parse,
        NormalizeStageResult normalize,
        ChunkStageResult chunk,
        String error
) {
}
