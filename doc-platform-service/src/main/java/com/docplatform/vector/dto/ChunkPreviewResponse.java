package com.docplatform.vector.dto;

import java.util.List;

public record ChunkPreviewResponse(
        int totalChunks,
        int sampleLength,
        List<ChunkPreviewItem> chunks) {

    public record ChunkPreviewItem(int index, int length, String content) {}
}
