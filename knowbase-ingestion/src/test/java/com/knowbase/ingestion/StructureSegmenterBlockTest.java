package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.status.ContentFamily;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StructureSegmenterBlockTest {

    private final StructureSegmenter segmenter = new StructureSegmenter();

    @Test
    void segmentsFromStructuralBlocksByHeadingSections() {
        ParsedDocument document = new ParsedDocument(
                "memory://doc.md",
                "Doc",
                "# Title\n\nFirst section.\n\n## Subtitle\n\nSecond section.",
                ContentFamily.RICH_TEXT,
                Map.of(),
                List.of(
                        StructuralBlock.heading(1, "Title", 0),
                        StructuralBlock.paragraph("First section.", 1),
                        StructuralBlock.heading(2, "Subtitle", 2),
                        StructuralBlock.paragraph("Second section.", 3)
                )
        );
        DocumentProfile profile = new DocumentProfile(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "default_markdown",
                ContentFamily.RICH_TEXT,
                "markdown-structure",
                "structure_token_window",
                null,
                Map.of(),
                Map.of(),
                true
        );

        List<StructuralSegment> segments = segmenter.segment(document, profile);

        assertTrue(segments.size() >= 2);
        assertTrue(segments.stream().anyMatch(segment -> segment.content().contains("First section")));
        assertTrue(segments.stream().anyMatch(segment -> segment.content().contains("Second section")));
    }
}
