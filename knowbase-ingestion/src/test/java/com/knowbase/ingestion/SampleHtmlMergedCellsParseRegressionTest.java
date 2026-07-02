package com.knowbase.ingestion;

import com.knowbase.ingestion.parse.ParsedDocumentParseEnricher;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SampleHtmlMergedCellsParseRegressionTest {

    @Test
    void htmlMergedCellsExposeSpanMetadata() throws Exception {
        byte[] bytes = SampleHtmlMergedCellsParseRegressionTest.class.getClassLoader()
                .getResourceAsStream("sample-documents/html/merged-cells.html")
                .readAllBytes();

        ParsedDocument parsed = ParsedDocumentParseEnricher.enrich(new HtmlStructureParser().parse(new DocumentSource(
                "memory://merged-cells.html",
                "merged-cells.html",
                "text/html",
                new ByteArrayInputStream(bytes),
                Map.of()
        )));

        assertTrue(parsed.structureAware());
        assertTrue(parsed.blocks().stream().anyMatch(block -> "table_row".equals(block.blockType())));
        assertTrue(parsed.blocks().stream().anyMatch(block -> hasMergedSpan(block.metadata())));
        assertTrue(parsed.blocks().stream()
                .filter(block -> "table_row".equals(block.blockType()))
                .anyMatch(block -> block.metadata().containsKey("cellCoordinates")));
        assertTrue(parsed.blocks().stream().allMatch(block -> block.metadata().containsKey("readingOrder")));
        assertTrue(parsed.blocks().stream().anyMatch(block -> "table_summary".equals(block.blockType())));
    }

    @SuppressWarnings("unchecked")
    private static boolean hasMergedSpan(Map<String, Object> metadata) {
        Object coordinates = metadata.get("cellCoordinates");
        if (!(coordinates instanceof List<?> cells)) {
            return false;
        }
        for (Object cell : cells) {
            if (cell instanceof Map<?, ?> map) {
                if (Integer.valueOf(2).equals(asInt(map.get("columnSpan")))
                        || Integer.valueOf(2).equals(asInt(map.get("rowSpan")))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Integer asInt(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }
}
