package com.knowbase.ingestion.external;

import com.knowbase.ingestion.ParsedDocument;
import com.knowbase.ingestion.parse.ParsedDocumentParseEnricher;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalParserResponseMapperTest {

    @Test
    void mapsDoclingStyleBlocksAndTables() throws Exception {
        String json = new String(ExternalParserResponseMapperTest.class.getClassLoader()
                .getResourceAsStream("sample-documents/external/mock-docling-response.json")
                .readAllBytes(), StandardCharsets.UTF_8);

        ParsedDocument parsed = ParsedDocumentParseEnricher.enrich(ExternalParserResponseMapper.map(
                "memory://report.pdf",
                "report.pdf",
                json,
                "docling",
                Map.of()
        ));

        assertTrue(parsed.structureAware());
        assertEquals("blocks", parsed.metadata().get("externalParserMapping"));
        assertTrue(parsed.blocks().stream().anyMatch(block -> block.content().contains("Quarterly Report")));
        assertTrue(parsed.blocks().stream().anyMatch(block -> "table_row".equals(block.blockType())));
        assertTrue(parsed.blocks().stream().anyMatch(block -> block.metadata().containsKey("evidenceAssetHint")));
        assertNotNull(parsed.metadata().get("externalParserImages"));
        assertNotNull(parsed.metadata().get("externalParserPages"));
    }
}
