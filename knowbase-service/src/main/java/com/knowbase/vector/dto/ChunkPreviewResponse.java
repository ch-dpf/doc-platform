package com.knowbase.vector.dto;

import java.util.List;

public record ChunkPreviewResponse(
        int totalChunks,
        int sampleLength,
        List<ChunkPreviewItem> chunks,
        /** 应用表头块过滤前的分块数 */
        int rawTotalChunks,
        /** 被表头块过滤移除的块数 */
        int filteredOutCount,
        String contentFamily,
        String chunkingStrategy,
        String chunkingAdjustmentReason,
        boolean multiGranularity,
        String chunkProfileId,
        boolean primaryProfile) {

    public ChunkPreviewResponse(
            int totalChunks,
            int sampleLength,
            List<ChunkPreviewItem> chunks,
            int rawTotalChunks,
            int filteredOutCount) {
        this(totalChunks, sampleLength, chunks, rawTotalChunks, filteredOutCount, null, null, null, false, null, false);
    }

    public record ChunkPreviewItem(int index, int length, String content) {}
}
