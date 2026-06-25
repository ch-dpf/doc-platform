package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentChunk;
import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.domain.status.ContentFamily;
import com.knowbase.ingestion.smart.SmartStructureDocumentChunker;
import com.knowbase.tokenizer.ApproximateTokenizer;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmartStructureDocumentChunkerTest {

    private final SmartStructureDocumentChunker chunker = new SmartStructureDocumentChunker();

    @Test
    void emitsParentChildSentenceWindowsForStructuredText() {
        UUID libraryId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        ParsedDocument document = new ParsedDocument(
                "memory://guide.md",
                "Guide",
                "",
                ContentFamily.RICH_TEXT,
                Map.of(),
                List.of(
                        StructuralBlock.heading(1, "Introduction", 0),
                        StructuralBlock.paragraph("First sentence about onboarding. Second sentence about goals.", 1),
                        StructuralBlock.paragraph("Third sentence with details. Fourth sentence with examples.", 2)
                )
        );
        LibraryProfile libraryProfile = libraryProfile(128, 16);
        DocumentProfile documentProfile = profile(Map.of("chunkEngine", "smart"));

        List<DocumentChunk> chunks = chunker.chunk(
                libraryId,
                documentId,
                UUID.randomUUID(),
                document,
                libraryProfile,
                documentProfile,
                new ApproximateTokenizer("approx-test", "1"),
                Map.of()
        );

        assertTrue(chunks.stream().anyMatch(chunk -> "parent".equals(chunk.metadata().get("chunkRole"))));
        assertTrue(chunks.stream().anyMatch(chunk -> "child".equals(chunk.metadata().get("chunkRole"))));
        assertTrue(chunks.stream().anyMatch(chunk -> "smart".equals(chunk.metadata().get("chunkEngine"))));
        assertTrue(chunks.stream().filter(chunk -> chunk.parentChunkId() != null).allMatch(chunk ->
                Boolean.TRUE.equals(chunk.metadata().get("indexable"))));
        assertTrue(chunks.stream().filter(chunk -> "parent".equals(chunk.metadata().get("chunkRole"))).allMatch(chunk ->
                Boolean.FALSE.equals(chunk.metadata().get("indexable"))));
    }

    @Test
    void shouldUseSmartEngineSkipsTableRowStrategy() {
        DocumentProfile tableProfile = profile(Map.of("chunkEngine", "smart"));
        assertFalse(SmartStructureDocumentChunker.shouldUseSmartEngine(
                new DocumentProfile(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "default_table",
                        ContentFamily.STRUCTURED_TABLE,
                        "table-deep",
                        "table_row_token_window",
                        null,
                        Map.of(),
                        Map.of("chunkEngine", "smart"),
                        true
                ),
                Map.of()
        ));
        assertTrue(SmartStructureDocumentChunker.shouldUseSmartEngine(tableProfile, Map.of()));
    }

    private static LibraryProfile libraryProfile(int chunkMaxTokens, int chunkOverlapTokens) {
        UUID libraryId = UUID.randomUUID();
        return new LibraryProfile(
                UUID.randomUUID(),
                libraryId,
                1,
                "ollama",
                "bge-m3",
                1024,
                null,
                chunkMaxTokens,
                chunkOverlapTokens,
                8,
                Map.of(),
                Instant.now()
        );
    }

    private static DocumentProfile profile(Map<String, Object> options) {
        UUID libraryId = UUID.randomUUID();
        return new DocumentProfile(
                UUID.randomUUID(),
                libraryId,
                "default_text",
                ContentFamily.PLAIN_TEXT,
                "text-structure",
                "paragraph_token_window",
                null,
                Map.of(),
                options,
                true
        );
    }
}
