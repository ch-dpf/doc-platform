package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentChunk;
import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.domain.status.ContentFamily;
import com.knowbase.tokenizer.ApproximateTokenizer;
import com.knowbase.tokenizer.DefaultTokenWindowChunker;
import com.knowbase.tokenizer.DefaultTokenizerRegistry;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructuredTableChunkPostProcessorTest {

    private final StructuredTableChunkPostProcessor postProcessor = new StructuredTableChunkPostProcessor();

    @Test
    void endToEndCsvChunksIncludeDocumentSummaryAndMergedRows() {
        ParsedDocument parsed = new StructuredTableDocumentParser().parse(new DocumentSource(
                "memory://weekly.csv",
                "weekly.csv",
                "text/csv",
                new java.io.ByteArrayInputStream("""
                        Region,Q1,Q2
                        APAC,10,12
                        EMEA,8,9
                        AMER,11,13
                        """.getBytes(StandardCharsets.UTF_8)),
                Map.of()
        ));
        DocumentProfile tableProfile = tableProfile(smartTableOptions());
        LibraryProfile libraryProfile = libraryProfile(384, 48);
        UUID libraryId = libraryProfile.libraryId();
        UUID documentId = UUID.randomUUID();
        UUID indexVersionId = UUID.randomUUID();
        ApproximateTokenizer tokenizer = new ApproximateTokenizer("approx-test", "1");

        List<DocumentChunk> rawChunks = new TokenBasedDocumentChunker(
                new DefaultTokenizerRegistry(),
                new DefaultTokenWindowChunker()
        ).chunk(
                libraryId,
                documentId,
                indexVersionId,
                parsed,
                libraryProfile,
                tableProfile,
                tokenizer,
                Map.of()
        );

        List<DocumentChunk> optimized = postProcessor.process(
                rawChunks,
                new ChunkPostProcessContext(parsed, libraryProfile, tableProfile, tokenizer, Map.of())
        );

        assertTrue(optimized.stream().noneMatch(chunk -> "table_summary".equals(chunk.chunkBoundaryType())));
        assertTrue(optimized.stream().anyMatch(chunk -> "document_summary".equals(chunk.chunkBoundaryType())));
        assertEquals(4, optimized.stream().filter(chunk -> "table_row".equals(chunk.chunkBoundaryType())).count());
    }

    @Test
    void singleSheetCsvGetsDocumentSummary() {
        ParsedDocument parsed = new StructuredTableDocumentParser().parse(new DocumentSource(
                "memory://weekly.csv",
                "weekly.csv",
                "text/csv",
                new java.io.ByteArrayInputStream("""
                        Region,Q1,Q2
                        APAC,10,12
                        """.getBytes(StandardCharsets.UTF_8)),
                Map.of()
        ));
        DocumentProfile tableProfile = tableProfile(smartTableOptions());
        LibraryProfile libraryProfile = libraryProfile(384, 48);
        ApproximateTokenizer tokenizer = new ApproximateTokenizer("approx-test", "1");
        List<DocumentChunk> rawChunks = new TokenBasedDocumentChunker(
                new DefaultTokenizerRegistry(),
                new DefaultTokenWindowChunker()
        ).chunk(
                libraryProfile.libraryId(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                parsed,
                libraryProfile,
                tableProfile,
                tokenizer,
                Map.of()
        );
        List<DocumentChunk> optimized = postProcessor.process(
                rawChunks,
                new ChunkPostProcessContext(parsed, libraryProfile, tableProfile, tokenizer, Map.of())
        );
        assertTrue(optimized.stream().anyMatch(chunk -> "document_summary".equals(chunk.chunkBoundaryType())));
    }

    private static Map<String, Object> smartTableOptions() {
        return Map.of(
                "chunkEngine", "smart",
                "chunkingStrategy", "table_row",
                "tableRowGroupMaxRows", 1,
                "mergeSmallRowChunks", false,
                "prependSheetContext", false,
                "emitDocumentSummary", true
        );
    }

    @Test
    void canDisablePostProcessingViaProfileOption() {
        ParsedDocument parsed = new ParsedDocument(
                "memory://one.csv",
                "one.csv",
                "Region=APAC | Q1=10",
                ContentFamily.STRUCTURED_TABLE,
                Map.of("tableFormat", "csv"),
                List.of(StructuralBlock.tableRow("Region=APAC | Q1=10", 0, 1))
        );
        DocumentProfile disabled = tableProfile(Map.of("tableChunkPostProcess", false));
        ChunkPostProcessContext context = new ChunkPostProcessContext(
                parsed,
                libraryProfile(384, 48),
                disabled,
                new ApproximateTokenizer("approx-test", "1"),
                Map.of()
        );

        DocumentChunk chunk = sampleChunk(parsed, "Region=APAC | Q1=10");
        List<DocumentChunk> result = postProcessor.process(List.of(chunk), context);

        assertEquals(1, result.size());
        assertEquals(chunk.content(), result.getFirst().content());
    }

    private static DocumentChunk sampleChunk(ParsedDocument document, String content) {
        return new DocumentChunk(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                content,
                8,
                "approx-test",
                "1",
                "bge-m3",
                "table_row",
                null,
                Map.of(
                        "indexable", true,
                        "chunkRole", "flat",
                        "flatOrdinal", 0,
                        "headerPath", List.of("Region", "Q1"),
                        "tableFormat", "csv"
                )
        );
    }

    private static DocumentProfile tableProfile(Map<String, Object> options) {
        return new DocumentProfile(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "default_table",
                ContentFamily.STRUCTURED_TABLE,
                "table-deep",
                "table_row_token_window",
                null,
                Map.of(),
                options,
                true
        );
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
                12,
                Map.of(),
                Instant.now()
        );
    }
}
