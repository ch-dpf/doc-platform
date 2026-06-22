package com.knowbase.ingestion;

import com.knowbase.domain.status.ContentFamily;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ParsedDocumentStructureEnricherTest {

    @Test
    void enrichesTikaPlainTextIntoParagraphBlocks() {
        ParsedDocument parsed = new ParsedDocument(
                "memory://doc.rtf",
                "Doc",
                "First paragraph.\n\nSecond paragraph.",
                ContentFamily.RICH_TEXT,
                Map.of("parser", "tika")
        );
        ParsedDocument enriched = ParsedDocumentStructureEnricher.enrich(parsed, "memory://doc.rtf");
        assertTrue(enriched.structureAware());
        assertTrue(enriched.blocks().size() >= 2);
    }

    @Test
    void enrichesMarkdownLikeText() {
        ParsedDocument parsed = new ParsedDocument(
                "memory://doc.bin",
                "Doc",
                "# Title\n\nBody text.",
                ContentFamily.RICH_TEXT,
                Map.of("parser", "tika")
        );
        ParsedDocument enriched = ParsedDocumentStructureEnricher.enrich(parsed, "memory://doc.bin");
        assertTrue(enriched.blocks().stream().anyMatch(block -> "heading".equals(block.blockType())));
    }
}
