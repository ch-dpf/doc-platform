package com.knowbase.vector.chunk;

import java.util.List;

public record ChunkPipelineResult(
        List<String> chunks,
        int rawTotalChunks,
        int filteredOutCount,
        String processedText) {}
