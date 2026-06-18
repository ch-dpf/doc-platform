package com.knowbase.domain.model;

public record IndexedChunk(
        DocumentChunk chunk,
        float[] embedding
) {
}
