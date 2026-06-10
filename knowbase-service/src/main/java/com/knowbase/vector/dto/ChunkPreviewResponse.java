package com.knowbase.vector.dto;

import java.util.List;

public record ChunkPreviewResponse(
        int totalChunks,
        int sampleLength,
        List<ChunkPreviewItem> chunks,
        /** 应用表头块过滤前的分块数 */
        int rawTotalChunks,
        /** 被表头块过滤移除的块数 */
        int filteredOutCount) {

    public record ChunkPreviewItem(int index, int length, String content) {}
}
