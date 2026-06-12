package com.knowbase.pipeline.chunk;

import java.util.List;

public record ChunkPipelineResult(
        List<PipelineChunk> pipelineChunks,
        int rawTotalChunks,
        int filteredOutCount,
        String processedText,
        String contentFamilyWire,
        String chunkingStrategyWire,
        String chunkingAdjustmentReason,
        boolean multiGranularity,
        String contentSignalsJson) {

    public ChunkPipelineResult(
            List<String> chunks, int rawTotalChunks, int filteredOutCount, String processedText) {
        this(
                chunks == null ? List.of() : chunks.stream().map(PipelineChunk::leaf).toList(),
                rawTotalChunks,
                filteredOutCount,
                processedText,
                null,
                null,
                null,
                false,
                null);
    }

    public List<String> chunks() {
        return pipelineChunks.stream().map(PipelineChunk::content).toList();
    }
}
