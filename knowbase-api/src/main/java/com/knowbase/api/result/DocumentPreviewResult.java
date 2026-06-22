package com.knowbase.api.result;

import java.util.List;

public record DocumentPreviewResult(
        String sourceUri,
        String title,
        String documentProfileCode,
        String parserCode,
        String contentFamily,
        int chunkCount,
        int indexableChunkCount,
        List<ChunkPreviewResult> chunks,
        ParseStageResult parse,
        NormalizeStageResult normalize,
        String error
) {
}
