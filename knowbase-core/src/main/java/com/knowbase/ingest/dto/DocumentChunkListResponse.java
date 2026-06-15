package com.knowbase.ingest.dto;

import java.util.List;
import java.util.UUID;

public record DocumentChunkListResponse(
        UUID docId,
        String fileName,
        int version,
        List<ChunkItem> items,
        long total,
        int page,
        int size) {

    public record ChunkItem(int index, int length, String content) {}
}
