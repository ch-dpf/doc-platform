package com.knowbase.pipeline.chunk;

import com.knowbase.vector.chunk.ChunkingStrategy;
import com.knowbase.vector.config.ChunkingProperties;
import com.knowbase.vector.service.ChunkingService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HierarchicalChunkerTest {

    @Test
    void splitsLongSectionIntoChildChunksWithParentContext() {
        ChunkingProperties props = new ChunkingProperties();
        props.setStrategy(ChunkingStrategy.HEADING_LEVEL);
        props.setChunkSize(120);
        props.setOverlap(20);
        props.setMinChunkSize(40);
        props.setMaxChunkSize(400);
        props.setMinParagraphLength(20);
        ChunkingService chunkingService = new ChunkingService(props, null);

        String sectionBody = "这是章节正文。".repeat(30);
        String text = "# 第一章\n\n" + sectionBody + "\n\n# 第二章\n\n短文。";

        List<PipelineChunk> chunks = HierarchicalChunker.chunk(null, text, props, chunkingService);

        assertTrue(chunks.size() > 2);
        assertTrue(chunks.stream().anyMatch(PipelineChunk::hasParentContext));
        assertTrue(chunks.stream().anyMatch(c -> c.content().contains("短文。")));
    }
}
