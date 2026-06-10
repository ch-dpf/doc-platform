package com.knowbase.vector.chunk;

import com.knowbase.vector.config.ChunkingProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticChunkerTest {

    @Test
    void groupsSentencesWhenSimilarityStaysHigh() {
        ChunkingProperties props = new ChunkingProperties();
        props.setSemanticSimilarityThreshold(0.5d);
        props.setChunkSize(1000);

        List<String> sentences = List.of("向量数据库介绍。", "pgvector 扩展说明。", "完全无关的烹饪笔记。");
        List<float[]> embeddings = List.of(
                new float[] {1f, 0f},
                new float[] {0.95f, 0.05f},
                new float[] {0f, 1f});

        List<String> groups = SemanticChunker.groupBySimilarity(sentences, embeddings, props);
        assertEquals(2, groups.size());
        assertEquals("向量数据库介绍。pgvector 扩展说明。", groups.get(0));
        assertEquals("完全无关的烹饪笔记。", groups.get(1));
    }

    @Test
    void mergeUndersizedGroupsCombinesShortLeadingChunk() {
        ChunkingProperties props = new ChunkingProperties();
        props.setMinChunkSize(80);
        List<String> merged = SemanticChunker.mergeUndersizedGroups(
                List.of("短开头。", "后续步骤说明。".repeat(5)), props);
        assertEquals(1, merged.size());
        assertTrue(merged.get(0).contains("短开头"));
        assertTrue(merged.get(0).contains("后续步骤"));
    }
}
