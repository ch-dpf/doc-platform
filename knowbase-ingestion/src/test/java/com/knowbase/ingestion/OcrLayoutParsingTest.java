package com.knowbase.ingestion;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OcrLayoutParsingTest {

    @Test
    void parsesOcrTextIntoLayoutBlocks() {
        String text = """
                第一章 总则

                这是第一段正文内容。

                列A    列B
                值1    值2
                """;
        List<StructuralBlock> blocks = StructureParsingSupport.parseOcrLayout(text, Map.of(
                "pageNumber", 3,
                "columnCount", 2,
                "ocrConfidence", 0.82
        ));
        assertTrue(blocks.size() >= 3);
        assertEquals("heading", blocks.getFirst().blockType());
        assertTrue(blocks.stream().anyMatch(block -> "table_row".equals(block.blockType())));
        assertTrue(blocks.stream().allMatch(block -> block.metadata().containsKey("bbox")));
        assertTrue(blocks.stream().allMatch(block -> block.metadata().containsKey("readingOrder")));
        assertTrue(blocks.stream().allMatch(block -> Integer.valueOf(3).equals(block.metadata().get("pageNumber"))));
        assertTrue(blocks.stream().allMatch(block -> Double.valueOf(0.82).equals(block.metadata().get("ocrConfidence"))));
        assertTrue(blocks.stream().allMatch(block -> Boolean.TRUE.equals(block.metadata().get("multiColumn"))));
    }

    @Test
    void parsesHocrConfidenceAndBbox() {
        String hocr = """
                <span class='ocr_line' title='bbox 10 20 110 40; x_wconf 93'>
                  <span class='ocrx_word'>Invoice</span>
                </span>
                """;

        List<StructuralBlock> blocks = OcrEngineOutputParser.parseHocr(hocr, Map.of("pageNumber", 2));

        assertEquals(1, blocks.size());
        assertEquals("Invoice", blocks.getFirst().content());
        assertEquals(0.93, (Double) blocks.getFirst().metadata().get("ocrConfidence"), 0.001);
        assertEquals(List.of(10d, 20d, 100d, 20d), blocks.getFirst().metadata().get("bbox"));
        assertEquals(2, blocks.getFirst().metadata().get("pageNumber"));
    }
}
