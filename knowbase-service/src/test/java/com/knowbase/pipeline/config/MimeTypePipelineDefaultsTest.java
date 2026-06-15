package com.knowbase.pipeline.config;

import com.knowbase.ingest.config.OcrProperties;
import com.knowbase.pipeline.content.ContentFamilyPipelineDefaults;
import com.knowbase.library.config.CleaningRulesSettings;
import com.knowbase.library.config.ParsingRulesSettings;
import com.knowbase.vector.chunk.ChunkingStrategy;
import com.knowbase.vector.config.ChunkingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MimeTypePipelineDefaultsTest {

    private MimeTypePipelineDefaults defaults;

    @BeforeEach
    void setUp() {
        defaults = new MimeTypePipelineDefaults(new ContentFamilyPipelineDefaults(new OcrProperties()));
    }

    @Test
    void applyExcelDefaults() {
        ParsingRulesSettings parsing = new ParsingRulesSettings();
        CleaningRulesSettings cleaning = new CleaningRulesSettings();
        ChunkingProperties chunking = new ChunkingProperties();
        chunking.setStrategy(ChunkingStrategy.SEMANTIC);
        defaults.apply("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", parsing, cleaning);
        assertEquals("text-only", parsing.getTableExtraction());
        assertEquals(ChunkingStrategy.SEMANTIC, chunking.getStrategy());
        assertFalse(cleaning.isRemoveHeaderFooter());
        assertTrue(cleaning.isRemoveDuplicateParagraphs());
    }

    @Test
    void applyWordDefaults() {
        ParsingRulesSettings parsing = new ParsingRulesSettings();
        CleaningRulesSettings cleaning = new CleaningRulesSettings();
        ChunkingProperties chunking = new ChunkingProperties();
        chunking.setStrategy(ChunkingStrategy.SEMANTIC);
        defaults.apply("application/vnd.openxmlformats-officedocument.wordprocessingml.document", parsing, cleaning);
        assertEquals("structured", parsing.getTableExtraction());
        assertEquals(ChunkingStrategy.SEMANTIC, chunking.getStrategy());
        assertTrue(cleaning.isRemoveHeaderFooter());
    }
}
