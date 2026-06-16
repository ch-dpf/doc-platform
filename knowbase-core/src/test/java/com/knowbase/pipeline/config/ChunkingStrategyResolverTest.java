package com.knowbase.pipeline.config;

import com.knowbase.vector.chunk.ChunkingStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChunkingStrategyResolverTest {

    @Test
    void autoResolvesPerFileType() {
        assertEquals(
                ChunkingStrategy.HEADING_LEVEL,
                ChunkingStrategyResolver.resolve(
                        ChunkingStrategy.AUTO,
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        assertEquals(
                ChunkingStrategy.PARAGRAPH_FIRST,
                ChunkingStrategyResolver.resolve(ChunkingStrategy.AUTO, "application/pdf"));
    }

    @Test
    void explicitStrategyOverridesFileTypeDefault() {
        assertEquals(
                ChunkingStrategy.SEMANTIC,
                ChunkingStrategyResolver.resolve(
                        ChunkingStrategy.SEMANTIC,
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
    }
}
