package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.status.ContentFamily;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StructureSegmenterTest {

    private final StructureSegmenter segmenter = new StructureSegmenter();

    @Test
    void segmentsMarkdownByHeading() {
        ParsedDocument document = new ParsedDocument(
                "memory://doc.md",
                "Doc",
                "# Title\n\nFirst section.\n\n## Subtitle\n\nSecond section.",
                ContentFamily.RICH_TEXT,
                Map.of()
        );
        DocumentProfile profile = new DocumentProfile(
                java.util.UUID.randomUUID(),
                java.util.UUID.randomUUID(),
                "default_markdown",
                ContentFamily.RICH_TEXT,
                "text",
                "structure_token_window",
                null,
                Map.of(),
                Map.of(),
                true
        );
        List<StructuralSegment> segments = segmenter.segment(document, profile);
        assertTrue(segments.size() >= 2);
        assertTrue(segments.stream().anyMatch(segment -> segment.content().contains("First section")));
    }

    @Test
    void segmentsTableRowsByStrategy() {
        ParsedDocument document = new ParsedDocument(
                "memory://table.csv",
                "Table",
                "col1,col2\nv1,v2\nv3,v4",
                ContentFamily.STRUCTURED_TABLE,
                Map.of()
        );
        DocumentProfile profile = new DocumentProfile(
                java.util.UUID.randomUUID(),
                java.util.UUID.randomUUID(),
                "default_table",
                ContentFamily.STRUCTURED_TABLE,
                "tika",
                "table_row_token_window",
                null,
                Map.of(),
                Map.of(),
                true
        );
        List<StructuralSegment> segments = segmenter.segment(document, profile);
        assertTrue(segments.size() >= 2);
    }
}
