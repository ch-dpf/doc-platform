package com.knowbase.vector.dto;

import java.util.UUID;

public record RagCitation(
        UUID chunkId,
        UUID docId,
        int chunkIndex,
        double score,
        String excerpt
) {
}
