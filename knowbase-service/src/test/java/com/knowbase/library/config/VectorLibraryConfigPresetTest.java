package com.knowbase.library.config;

import com.knowbase.platform.JsonSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class VectorLibraryConfigPresetTest {

    @Test
    void roundTripsLibraryPresetIdWeeklyReportExcel() {
        VectorLibraryConfig config = new VectorLibraryConfig();
        config.setLibraryPresetId("weekly-report-excel");
        config.setChunkingStrategy(com.knowbase.vector.chunk.ChunkingStrategy.PARAGRAPH_FIRST);
        config.getParsing().setTableExtraction("text-only");

        String json = JsonSupport.toJson(config);
        VectorLibraryConfig parsed = JsonSupport.parseLibraryConfig(json);

        assertEquals("weekly-report-excel", parsed.getLibraryPresetId());
        assertEquals(com.knowbase.vector.chunk.ChunkingStrategy.PARAGRAPH_FIRST, parsed.getChunkingStrategy());
    }

    @Test
    void roundTripsLibraryPresetIdCustom() {
        VectorLibraryConfig config = new VectorLibraryConfig();
        config.setLibraryPresetId("custom");

        String json = JsonSupport.toJson(config);
        VectorLibraryConfig parsed = JsonSupport.parseLibraryConfig(json);

        assertEquals("custom", parsed.getLibraryPresetId());
    }

    @Test
    void emptyConfigJsonYieldsNullLibraryPresetId() {
        VectorLibraryConfig parsed = JsonSupport.parseLibraryConfig("");
        assertNull(parsed.getLibraryPresetId());
    }

    @Test
    void blankConfigJsonYieldsNullLibraryPresetId() {
        VectorLibraryConfig parsed = JsonSupport.parseLibraryConfig("   ");
        assertNull(parsed.getLibraryPresetId());
    }
}
