package com.knowbase.library.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class VectorLibraryConfigFactoryTest {

    private static final List<String> GLOBAL_MIMES = List.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain",
            "text/markdown",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    @Test
    void defaultsAlignWithProductPreset() {
        VectorLibraryConfig cfg = VectorLibraryConfigFactory.defaults(GLOBAL_MIMES);

        assertEquals(500, cfg.getChunkSize());
        assertEquals(120, cfg.getChunkOverlap());
        assertEquals("ollama", cfg.getEmbeddingProvider());
        assertEquals("nomic-embed-text", cfg.getEmbeddingModel());
        assertEquals(768, cfg.getEmbeddingDimension());
        assertTrue(cfg.isHierarchicalChunkingEnabled());

        RetrievalRulesSettings retrieval = cfg.getRetrieval();
        assertTrue(retrieval.isHybridSearchEnabled());
        assertTrue(retrieval.isRerankEnabled());
        assertEquals(0.4, retrieval.getSimilarityThreshold(), 0.001);
        assertEquals(12, retrieval.getDefaultTopK());

        assertEquals("zh-CN", cfg.getParsing().getDefaultLanguage());
        assertTrue(cfg.getParsing().isAutoDetectEncoding());
        assertEquals(5, cfg.getParserRules().size());
        assertTrue(cfg.getParserRules().stream().allMatch(r -> "auto".equals(r.getParserId())));
    }
}
