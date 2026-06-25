package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentChunk;
import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.domain.status.ContentFamily;
import com.knowbase.tokenizer.ApproximateTokenizer;
import com.knowbase.tokenizer.DefaultTokenWindowChunker;
import com.knowbase.tokenizer.DefaultTokenizerRegistry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenBasedDocumentChunkerTest {

    @Test
    void chunkEngineSmartUsesSentenceWindowChildren() {
        UUID libraryId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        ParsedDocument document = new ParsedDocument(
                "memory://guide.md",
                "Guide",
                "",
                ContentFamily.RICH_TEXT,
                Map.of(),
                List.of(
                        StructuralBlock.heading(1, "Overview", 0),
                        StructuralBlock.paragraph("Alpha sentence one. Alpha sentence two.", 1),
                        StructuralBlock.paragraph("Beta sentence three. Beta sentence four.", 2)
                )
        );
        LibraryProfile libraryProfile = new LibraryProfile(
                UUID.randomUUID(),
                libraryId,
                1,
                "ollama",
                "bge-m3",
                1024,
                null,
                128,
                16,
                8,
                Map.of(),
                Instant.now()
        );
        DocumentProfile documentProfile = new DocumentProfile(
                UUID.randomUUID(),
                libraryId,
                "default_markdown",
                ContentFamily.RICH_TEXT,
                "markdown-structure",
                "structure_token_window",
                null,
                Map.of(),
                Map.of("chunkEngine", "smart"),
                true
        );

        List<DocumentChunk> chunks = new TokenBasedDocumentChunker(
                new DefaultTokenizerRegistry(),
                new DefaultTokenWindowChunker()
        ).chunk(
                libraryId,
                documentId,
                UUID.randomUUID(),
                document,
                libraryProfile,
                documentProfile,
                new ApproximateTokenizer("approx-test", "1"),
                Map.of()
        );

        assertTrue(chunks.stream().anyMatch(chunk -> "smart".equals(chunk.metadata().get("chunkEngine"))));
        assertTrue(chunks.stream().anyMatch(chunk -> "semantic+sentence-window".equals(chunk.metadata().get("strategy"))
                || "pdf-page-section-hybrid".equals(chunk.metadata().get("strategy"))));
    }

    @Test
    void smartDefaultsUseTokenBudgetAfterCharacterFallback() {
        UUID libraryId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        ParsedDocument document = new ParsedDocument(
                "memory://long.txt",
                "Long text",
                "段落" + "内容".repeat(160),
                ContentFamily.PLAIN_TEXT,
                Map.of(),
                List.of(StructuralBlock.paragraph("段落" + "内容".repeat(160), 0))
        );
        LibraryProfile libraryProfile = new LibraryProfile(
                UUID.randomUUID(),
                libraryId,
                1,
                "ollama",
                "bge-m3",
                1024,
                null,
                16,
                2,
                8,
                Map.of(),
                Instant.now()
        );
        DocumentProfile documentProfile = new DocumentProfile(
                UUID.randomUUID(),
                libraryId,
                "default_text",
                ContentFamily.PLAIN_TEXT,
                "text-structure",
                "paragraph_token_window",
                null,
                Map.of(),
                Map.of(),
                true
        );

        List<DocumentChunk> chunks = new TokenBasedDocumentChunker(
                new DefaultTokenizerRegistry(),
                new DefaultTokenWindowChunker()
        ).chunk(
                libraryId,
                documentId,
                UUID.randomUUID(),
                document,
                libraryProfile,
                documentProfile,
                new ApproximateTokenizer("approx-test", "1"),
                Map.of("segmentationMode", "smart")
        );

        assertTrue(chunks.size() > 1);
        assertTrue(chunks.stream().anyMatch(chunk -> "parent".equals(chunk.metadata().get("chunkRole"))));
        assertTrue(chunks.stream().anyMatch(chunk -> "child".equals(chunk.metadata().get("chunkRole"))));
        assertTrue(chunks.stream().filter(chunk -> "child".equals(chunk.metadata().get("chunkRole")))
                .allMatch(chunk -> chunk.tokenCount() <= 16));
        assertTrue(chunks.stream().filter(chunk -> "parent".equals(chunk.metadata().get("chunkRole")))
                .allMatch(chunk -> Boolean.FALSE.equals(chunk.metadata().get("indexable"))));
    }
}
