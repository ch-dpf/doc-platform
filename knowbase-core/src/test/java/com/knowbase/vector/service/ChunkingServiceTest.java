package com.knowbase.vector.service;

import com.knowbase.vector.chunk.ChunkingStrategy;
import com.knowbase.vector.chunk.SemanticChunker;
import com.knowbase.vector.config.ChunkingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ChunkingServiceTest {

    @Mock
    private SemanticChunker semanticChunker;

    private ChunkingProperties props;
    private ChunkingService service;

    @BeforeEach
    void setUp() {
        props = new ChunkingProperties();
        service = new ChunkingService(props, semanticChunker);
    }

    @Test
    void fixedCharChunksLongTextWithOverlap() {
        props.setStrategy(ChunkingStrategy.FIXED_CHAR);
        props.setChunkSize(50);
        props.setOverlap(10);
        props.setMinChunkSize(1);
        String text = "a".repeat(120);
        List<String> chunks = service.chunk(text);
        assertTrue(chunks.size() >= 2);
        assertTrue(chunks.get(0).length() <= 50);
    }

    @Test
    void paragraphFirstKeepsShortParagraphsTogether() {
        props.setStrategy(ChunkingStrategy.PARAGRAPH_FIRST);
        props.setChunkSize(600);
        props.setMinParagraphLength(100);
        props.setMinChunkSize(10);
        String text = "第一段说明 pgvector。\n\n第二段说明 Kafka 事件。\n\n第三段说明 MinIO。";
        List<String> chunks = service.chunk(text);
        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).contains("pgvector"));
        assertTrue(chunks.get(0).contains("Kafka"));
    }

    @Test
    void headingLevelSplitsBySections() {
        props.setStrategy(ChunkingStrategy.HEADING_LEVEL);
        props.setChunkSize(600);
        props.setMinChunkSize(1);
        String text = "# 第一章\n内容 A。\n\n## 第二章\n内容 B。";
        List<String> chunks = service.chunk(text);
        assertEquals(2, chunks.size());
        assertTrue(chunks.get(0).contains("内容 A"));
        assertTrue(chunks.get(1).contains("内容 B"));
    }
}
