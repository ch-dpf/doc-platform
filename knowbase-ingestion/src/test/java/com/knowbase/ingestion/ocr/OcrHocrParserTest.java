package com.knowbase.ingestion.ocr;

import com.knowbase.ingestion.StructuralBlock;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OcrHocrParserTest {

    @Test
    void parsesWordLevelMetadataInsideLines() {
        String hocr = """
                <span class="ocr_line" title="bbox 10 20 180 24; x_wconf 90">
                  <span class="ocrx_word" title="bbox 10 20 60 24; x_wconf 92">Hello</span>
                  <span class="ocrx_word" title="bbox 80 20 110 24; x_wconf 88">World</span>
                </span>
                """;
        List<StructuralBlock> blocks = OcrHocrParser.parse(hocr, Map.of("pageNumber", 1));
        assertEquals(1, blocks.size());
        Object words = blocks.getFirst().metadata().get("ocrWords");
        assertTrue(words instanceof List<?> list && list.size() == 2);
        assertEquals("Hello World", blocks.getFirst().content());
    }
}
