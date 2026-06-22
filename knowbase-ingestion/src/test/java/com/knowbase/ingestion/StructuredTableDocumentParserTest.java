package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.status.ContentFamily;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructuredTableDocumentParserTest {

    @Test
    void parsesCsvAsTableRowBlocks() {
        ParsedDocument parsed = new StructuredTableDocumentParser().parse(new DocumentSource(
                "memory://sales.csv",
                "sales.csv",
                "text/csv",
                new ByteArrayInputStream("""
                        Region,Q1,Q2
                        APAC,10,12
                        EMEA,8,9
                        """.getBytes(StandardCharsets.UTF_8)),
                Map.of("filename", "sales.csv")
        ));

        assertEquals(ContentFamily.STRUCTURED_TABLE, parsed.contentFamily());
        assertEquals("table-deep", parsed.metadata().get("parser"));
        assertEquals(2, parsed.metadata().get("rowGroupCount"));
        assertTrue(parsed.structureAware());
        assertEquals(2, parsed.blocks().size());
        assertTrue(parsed.blocks().stream().allMatch(block -> "table_row".equals(block.blockType())));
        assertTrue(parsed.blocks().getFirst().content().contains("Region=APAC"));
        assertTrue(parsed.blocks().getFirst().content().contains("Q1=10"));
    }

    @Test
    void tableRowsBecomeSemanticSegments() {
        ParsedDocument parsed = new StructuredTableDocumentParser().parse(new DocumentSource(
                "memory://sales.csv",
                "sales.csv",
                "text/csv",
                new ByteArrayInputStream("""
                        Region,Q1
                        APAC,10
                        EMEA,8
                        """.getBytes(StandardCharsets.UTF_8)),
                Map.of()
        ));
        DocumentProfile tableProfile = new DocumentProfile(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "default_table",
                ContentFamily.STRUCTURED_TABLE,
                "table-deep",
                "table_row_token_window",
                null,
                Map.of(),
                Map.of(),
                true
        );

        List<StructuralSegment> segments = new StructureSegmenter().segment(parsed, tableProfile);

        assertEquals(2, segments.size());
        assertTrue(segments.stream().allMatch(segment -> "table_row".equals(segment.boundaryType())));
    }
}
