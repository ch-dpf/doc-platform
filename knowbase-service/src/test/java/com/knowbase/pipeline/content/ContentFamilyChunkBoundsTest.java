package com.knowbase.pipeline.content;

import com.knowbase.vector.config.ChunkingProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContentFamilyChunkBoundsTest {

    private final ContentFamilyChunkBounds bounds = new ContentFamilyChunkBounds();

    @Test
    void clampsTabularChunkSizeToFamilyRange() {
        ChunkingProperties chunking = new ChunkingProperties();
        chunking.setChunkSize(1200);
        chunking.setOverlap(400);
        chunking.setMaxChunkSize(1600);

        bounds.apply(ContentFamily.TABULAR, chunking);

        assertEquals(700, chunking.getChunkSize());
        assertEquals(700, chunking.getMaxChunkSize());
    }
}
