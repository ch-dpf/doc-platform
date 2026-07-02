package com.knowbase.ingestion.pdf;

import com.knowbase.ingestion.StructuralBlock;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisionDocumentBlockMapperTest {

    @Test
    void mapsMarkdownHeadingsAndTableRows() {
        String pageText = """
                # Quarterly Report

                Revenue increased year over year.

                | Product | Q1 | Q2 |
                | --- | --- | --- |
                | Alpha | 10 | 20 |
                """;

        List<StructuralBlock> blocks = VisionDocumentBlockMapper.fromPageText(
                pageText,
                2,
                595d,
                842d,
                Map.of("visionDocumentModel", "test-vl")
        );

        assertTrue(blocks.size() >= 2);
        assertEquals("heading", blocks.getFirst().blockType());
        assertEquals(2, blocks.getFirst().metadata().get("pageNumber"));
        assertEquals("vision-vl", blocks.getFirst().metadata().get("ocrEngine"));
        assertTrue(blocks.stream().anyMatch(block -> "table_row".equals(block.blockType())));
        assertTrue(blocks.stream()
                .filter(block -> "table_row".equals(block.blockType()))
                .allMatch(block -> block.metadata().containsKey("tableRegionId")));
    }
}
