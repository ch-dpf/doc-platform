package com.knowbase.ingestion.testsupport;

import com.knowbase.domain.model.DocumentChunk;
import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.domain.status.ContentFamily;
import com.knowbase.ingestion.DocumentParser;
import com.knowbase.ingestion.DocumentPreparationPipeline;
import com.knowbase.ingestion.DocumentPreparationResult;
import com.knowbase.ingestion.DocumentSource;
import com.knowbase.ingestion.DocumentSourceLoader;
import com.knowbase.ingestion.DocumentTextNormalizer;
import com.knowbase.ingestion.MarkdownStructureParser;
import com.knowbase.ingestion.ParsedDocument;
import com.knowbase.ingestion.PdfLayoutParser;
import com.knowbase.ingestion.PreparationStage;
import com.knowbase.ingestion.StructuredTableDocumentParser;
import com.knowbase.ingestion.TextStructureParser;
import com.knowbase.ingestion.TokenBasedDocumentChunker;
import com.knowbase.ingestion.eval.IngestionCitationCompletenessEvaluator;
import com.knowbase.ingestion.parse.ParsedDocumentParseEnricher;
import com.knowbase.tokenizer.ApproximateTokenizer;
import com.knowbase.tokenizer.DefaultTokenWindowChunker;
import com.knowbase.tokenizer.DefaultTokenizerRegistry;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class IngestionEvalHarness {

    private static final int SNAPSHOT_CHUNK_MAX_TOKENS = 48;
    private static final int SNAPSHOT_CHUNK_OVERLAP = 8;

    private IngestionEvalHarness() {
    }

    public record DocumentMetrics(
            String fixtureId,
            double citationScore,
            int blockCount,
            int tableRowCount,
            ChunkSnapshotSignature.Signature chunkSignature
    ) {
        public Map<String, Object> toBaselineMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("minimumCitationScore", citationScore);
            map.put("blockCount", blockCount);
            map.put("tableRowCount", tableRowCount);
            map.put("indexableChunkCount", chunkSignature.indexableChunks());
            map.put("totalChunkCount", chunkSignature.totalChunks());
            map.put("maxIndexableTokens", chunkSignature.maxIndexableTokens());
            map.put("indexableFingerprints", chunkSignature.indexableFingerprints());
            return Map.copyOf(map);
        }
    }

    public static DocumentMetrics evaluatePdfFixture(String fixtureId) {
        return evaluateProgrammaticFixture(
                fixtureId,
                new PdfLayoutParser(),
                ContentFamily.RICH_TEXT,
                PdfLayoutParser.PARSER_CODE,
                "page_token_window"
        );
    }

    public static DocumentMetrics evaluateXlsxFixture(String fixtureId) {
        return evaluateProgrammaticFixture(
                fixtureId,
                new StructuredTableDocumentParser(),
                ContentFamily.STRUCTURED_TABLE,
                "table-deep",
                "table_row_token_window"
        );
    }

    private static DocumentMetrics evaluateProgrammaticFixture(
            String fixtureId,
            DocumentParser parser,
            ContentFamily contentFamily,
            String parserCode,
            String chunkingStrategy
    ) {
        byte[] bytes = IngestionEvalFixtureFactory.bytes(fixtureId);
        ParsedDocument parsed = ParsedDocumentParseEnricher.enrich(parser.parse(new DocumentSource(
                "memory://" + IngestionEvalFixtureFactory.filename(fixtureId),
                IngestionEvalFixtureFactory.filename(fixtureId),
                IngestionEvalFixtureFactory.mimeType(fixtureId),
                new ByteArrayInputStream(bytes),
                IngestionEvalFixtureFactory.metadata(fixtureId)
        )));
        return metricsFromParsed(
                fixtureId,
                parsed,
                "memory://" + IngestionEvalFixtureFactory.filename(fixtureId),
                contentFamily,
                parserCode,
                chunkingStrategy
        );
    }

    private static DocumentMetrics metricsFromParsed(
            String fixtureId,
            ParsedDocument parsed,
            String sourceUri,
            ContentFamily contentFamily,
            String parserCode,
            String chunkingStrategy
    ) {
        DocumentPreparationResult prepared = prepareParsed(parsed, sourceUri, contentFamily, parserCode, chunkingStrategy);
        IngestionCitationCompletenessEvaluator.DocumentScore citation =
                IngestionCitationCompletenessEvaluator.evaluate(prepared.parsed());
        ChunkSnapshotSignature.Signature signature = ChunkSnapshotSignature.capture(
                prepared.chunks(),
                IngestionEvalHarness::isIndexable
        );
        return new DocumentMetrics(
                fixtureId,
                citation.overallScore(),
                citation.blockCount(),
                citation.tableRowCount(),
                signature
        );
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
                "eval-" + parserCode,
                contentFamily,
                parserCode,
                chunkingStrategy,
                null,
                Map.of(),
                profileOptions(contentFamily, parserCode),
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
                chunkingOptions(contentFamily, parserCode)
        );
    }

    private static Map<String, Object> profileOptions(ContentFamily contentFamily, String parserCode) {
        if (PdfLayoutParser.PARSER_CODE.equals(parserCode)) {
            return Map.of("chunkEngine", "smart");
        }
        if (contentFamily == ContentFamily.STRUCTURED_TABLE) {
            return Map.of("chunkEngine", "smart");
        }
        return Map.of();
    }

    private static Map<String, Object> chunkingOptions(ContentFamily contentFamily, String parserCode) {
        if (contentFamily == ContentFamily.STRUCTURED_TABLE) {
            return Map.of("segmentationMode", "smart");
        }
        if (PdfLayoutParser.PARSER_CODE.equals(parserCode)) {
            return Map.of("segmentationMode", "smart", "chunkEngine", "smart");
        }
        return Map.of("segmentationMode", "smart");
    }

    private static boolean isIndexable(DocumentChunk chunk) {
        Object indexable = chunk.metadata() == null ? null : chunk.metadata().get("indexable");
        if (indexable instanceof Boolean value) {
            return value;
        }
        return chunk.parentChunkId() != null || chunk.metadata() == null || !chunk.metadata().containsKey("indexable");
    }
}
