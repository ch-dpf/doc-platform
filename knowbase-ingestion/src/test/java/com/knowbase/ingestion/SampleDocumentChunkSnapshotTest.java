package com.knowbase.ingestion;

import com.knowbase.ingestion.parse.ParsedDocumentParseEnricher;
import com.knowbase.domain.model.DocumentChunk;
import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.domain.status.ContentFamily;
import com.knowbase.tokenizer.ApproximateTokenizer;
import com.knowbase.tokenizer.DefaultTokenWindowChunker;
import com.knowbase.tokenizer.DefaultTokenizerRegistry;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SampleDocumentChunkSnapshotTest {

    private static final int SNAPSHOT_CHUNK_MAX_TOKENS = 48;
    private static final int SNAPSHOT_CHUNK_OVERLAP = 8;

    @Test
    void markdownGuideProducesStableChunkSignature() throws Exception {
        ChunkSnapshot snapshot = snapshotSample("sample-documents/markdown/guide.md", "text/markdown");
        assertEquals(6, snapshot.indexableCount());
        assertTrue(snapshot.parsedText().contains("KnowBase Guide"));
        assertTrue(snapshot.parsedText().contains("Install PostgreSQL"));
    }

    @Test
    void longPlainTextSplitsIntoMultipleChunks() throws Exception {
        ChunkSnapshot snapshot = snapshotSample("sample-documents/plain/long-paragraph.txt", "text/plain");
        assertTrue(snapshot.indexableCount() >= 2, "expected token window split, got " + snapshot.indexableCount());
        assertTrue(snapshot.indexableChunks().stream().allMatch(chunk -> chunk.tokenCount() <= SNAPSHOT_CHUNK_MAX_TOKENS));
    }

    @Test
    void markdownFaqOutlinePreservesHeadingBlocks() throws Exception {
        ChunkSnapshot snapshot = snapshotSample("sample-documents/markdown/faq-outline.md", "text/markdown");
        assertTrue(snapshot.indexableCount() >= 4);
        assertTrue(snapshot.parsedText().contains("Product FAQ"));
        assertTrue(snapshot.parsedText().contains("retrieval test"));
    }

    @Test
    void tableMetricsCsvProducesTableRowChunks() throws Exception {
        ChunkSnapshot snapshot = snapshotTableSample("sample-documents/table/metrics.csv", "text/csv");
        assertTrue(snapshot.indexableCount() >= 1);
        assertTrue(snapshot.indexableChunks().stream().anyMatch(chunk -> chunk.content().contains("张三")));
    }

    @Test
    void ocrHocrProducesBlocksReadyForChunking() throws Exception {
        URI uri = SampleDocumentChunkSnapshotTest.class.getClassLoader()
                .getResource("sample-documents/ocr/sample-scan.hocr").toURI();
        String hocr = Files.readString(Path.of(uri));
        ParsedDocument parsed = ParsedDocumentParseEnricher.enrich(new ParsedDocument(
                uri.toString(),
                "sample-scan.hocr",
                hocr,
                ContentFamily.IMAGE_TEXT,
                Map.of("parserCode", "ocr-layout"),
                com.knowbase.ingestion.ocr.OcrConfidencePolicy.apply(
                        com.knowbase.ingestion.ocr.OcrBlockFactory.fromHocr(hocr, Map.of("pageNumber", 1)),
                        0.6d
                )
        ));
        assertTrue(parsed.blocks().stream().anyMatch(block -> block.metadata().containsKey("ocrConfidence")));
        assertTrue(parsed.blocks().stream().anyMatch(block -> block.metadata().containsKey("evidenceAssetHint")));
    }

    private static ChunkSnapshot snapshotTableSample(String resourcePath, String mimeType) throws Exception {
        URI uri = SampleDocumentChunkSnapshotTest.class.getClassLoader().getResource(resourcePath).toURI();
        Path path = Path.of(uri);
        try (InputStream inputStream = Files.newInputStream(path)) {
            ParsedDocument parsed = ParsedDocumentParseEnricher.enrich(new StructuredTableDocumentParser().parse(new DocumentSource(
                    path.toUri().toString(),
                    path.getFileName().toString(),
                    mimeType,
                    inputStream,
                    Map.of()
            )));
            return snapshotFromParsed(
                    parsed,
                    path.toUri().toString(),
                    ContentFamily.STRUCTURED_TABLE,
                    "table-deep",
                    "table_row_token_window"
            );
        }
    }

    private static ChunkSnapshot snapshotFromParsed(ParsedDocument parsed, String sourceUri) {
        return snapshotFromParsed(parsed, sourceUri, ContentFamily.PLAIN_TEXT, "markdown-structure", "paragraph_token_window");
    }

    private static ChunkSnapshot snapshotFromParsed(
            ParsedDocument parsed,
            String sourceUri,
            ContentFamily contentFamily,
            String parserCode,
            String chunkingStrategy
    ) {
        DocumentPreparationResult prepared = prepareParsed(parsed, sourceUri, contentFamily, parserCode, chunkingStrategy);
        List<DocumentChunk> allChunks = prepared.chunks();
        List<DocumentChunk> indexable = allChunks.stream().filter(SampleDocumentChunkSnapshotTest::isIndexable).toList();
        return new ChunkSnapshot(allChunks, indexable, prepared.parsed().text());
    }

    private record ChunkSnapshot(List<DocumentChunk> allChunks, List<DocumentChunk> indexableChunks, String parsedText) {
        int indexableCount() {
            return indexableChunks.size();
        }
    }

    private static ChunkSnapshot snapshotSample(String resourcePath, String mimeType) throws Exception {
        URI uri = SampleDocumentChunkSnapshotTest.class.getClassLoader().getResource(resourcePath).toURI();
        Path path = Path.of(uri);
        String sourceUri = path.toUri().toString();
        DocumentParser parser = resourcePath.endsWith(".md")
                ? new MarkdownStructureParser()
                : new TextStructureParser();
        try (InputStream inputStream = Files.newInputStream(path)) {
            ParsedDocument parsed = parser.parse(new DocumentSource(
                    sourceUri,
                    path.getFileName().toString(),
                    mimeType,
                    inputStream,
                    Map.of()
            ));
            DocumentPreparationResult prepared = prepareParsed(parsed, sourceUri);
            List<DocumentChunk> allChunks = prepared.chunks();
            List<DocumentChunk> indexable = allChunks.stream().filter(SampleDocumentChunkSnapshotTest::isIndexable).toList();
            return new ChunkSnapshot(allChunks, indexable, prepared.parsed().text());
        }
    }

    private static List<DocumentChunk> chunkSample(String resourcePath, String mimeType) throws Exception {
        return snapshotSample(resourcePath, mimeType).allChunks();
    }

    private static DocumentPreparationResult prepareParsed(ParsedDocument parsed, String sourceUri) {
        return prepareParsed(parsed, sourceUri, ContentFamily.PLAIN_TEXT, "markdown-structure", "paragraph_token_window");
    }

    private static DocumentPreparationResult prepareParsed(
            ParsedDocument parsed,
            String sourceUri,
            ContentFamily contentFamily,
            String parserCode,
            String chunkingStrategy
    ) {
        UUID libraryId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID generationId = UUID.randomUUID();
        LibraryProfile libraryProfile = new LibraryProfile(
                UUID.randomUUID(),
                libraryId,
                1,
                "ollama",
                "bge-m3",
                1024,
                null,
                SNAPSHOT_CHUNK_MAX_TOKENS,
                SNAPSHOT_CHUNK_OVERLAP,
                8,
                Map.of(),
                Instant.now()
        );
        DocumentProfile documentProfile = new DocumentProfile(
                UUID.randomUUID(),
                libraryId,
                "default_text",
                contentFamily,
                parserCode,
                chunkingStrategy,
                null,
                Map.of(),
                Map.of(),
                true
        );
        DocumentPreparationPipeline pipeline = new DocumentPreparationPipeline(
                new DocumentSourceLoader(null, List.of(new MarkdownStructureParser(), new TextStructureParser())),
                new DocumentTextNormalizer(),
                new TokenBasedDocumentChunker(new DefaultTokenizerRegistry(), new DefaultTokenWindowChunker())
        );
        return pipeline.prepareFromParsed(
                parsed,
                sourceUri,
                libraryId,
                documentId,
                generationId,
                libraryProfile,
                documentProfile,
                new ApproximateTokenizer("approx-test", "1"),
                PreparationStage.CHUNK,
                Map.of("segmentationMode", "smart")
        );
    }

    private static boolean isIndexable(DocumentChunk chunk) {
        Object indexable = chunk.metadata() == null ? null : chunk.metadata().get("indexable");
        if (indexable instanceof Boolean value) {
            return value;
        }
        return chunk.parentChunkId() != null || chunk.metadata() == null || !chunk.metadata().containsKey("indexable");
    }
}
