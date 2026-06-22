package com.knowbase.ingestion;

import org.junit.jupiter.api.Test;

import java.util.List;

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
        List<StructuralBlock> blocks = StructureParsingSupport.parseOcrLayout(text);
        assertTrue(blocks.size() >= 3);
        assertEquals("heading", blocks.getFirst().blockType());
        assertTrue(blocks.stream().anyMatch(block -> "table_row".equals(block.blockType())));
    }
}
