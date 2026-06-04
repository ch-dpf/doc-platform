package com.docplatform.vector.service;

import com.docplatform.vector.chunk.ChunkingStrategy;
import com.docplatform.vector.config.ChunkingProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkingServiceTest {

    @Test
    void fixedCharChunksLongTextWithOverlap() {
        ChunkingProperties props = new ChunkingProperties();
        props.setStrategy(ChunkingStrategy.FIXED_CHAR);
        props.setChunkSize(50);
        props.setOverlap(10);
        props.setMinChunkSize(1);
        ChunkingService service = new ChunkingService(props);
        String text = "a".repeat(120);
        List<String> chunks = service.chunk(text);
        assertTrue(chunks.size() >= 2);
        assertTrue(chunks.get(0).length() <= 50);
    }

    @Test
    void paragraphFirstKeepsShortParagraphsTogether() {
        ChunkingProperties props = new ChunkingProperties();
        props.setStrategy(ChunkingStrategy.PARAGRAPH_FIRST);
        props.setChunkSize(600);
        props.setMinParagraphLength(100);
        props.setMinChunkSize(10);
        ChunkingService service = new ChunkingService(props);
        String text = "第一段说明 pgvector。\n\n第二段说明 Kafka 事件。\n\n第三段说明 MinIO。";
        List<String> chunks = service.chunk(text);
        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).contains("pgvector"));
        assertTrue(chunks.get(0).contains("Kafka"));
    }

    @Test
    void paragraphFirstSplitsOversizedParagraph() {
        ChunkingProperties props = new ChunkingProperties();
        props.setStrategy(ChunkingStrategy.PARAGRAPH_FIRST);
        props.setChunkSize(100);
        props.setOverlap(20);
        props.setMinChunkSize(10);
        props.setMinParagraphLength(5);
        ChunkingService service = new ChunkingService(props);
        String text = "x".repeat(250);
        List<String> chunks = service.chunk(text);
        assertTrue(chunks.size() >= 2);
    }
}
