package com.knowbase.api.result;

import java.util.List;

public record ChunkStageResult(
        int chunkCount,
        int indexableChunkCount,
        List<ChunkPreviewResult> chunks
) {
}
