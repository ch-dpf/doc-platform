package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.domain.observability.PipelineObserver;
import com.knowbase.domain.status.ContentFamily;
import com.knowbase.tokenizer.ApproximateTokenizer;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentPreparationPipelineTraceTest {

    @Test
    void prepareEmitsStageSpansThroughPostProcess() {
        RecordingPipelineObserver observer = new RecordingPipelineObserver();
        DocumentPreparationPipeline pipeline = new DocumentPreparationPipeline(
                new DocumentSourceLoader(null, List.of(new StructuredTableDocumentParser())),
                new DocumentTextNormalizer(),
                new TokenBasedDocumentChunker(
                        new com.knowbase.tokenizer.DefaultTokenizerRegistry(),
                        new com.knowbase.tokenizer.DefaultTokenWindowChunker()
                ),
                new DefaultDocumentMetadataEnricher(),
                CompositeChunkPostProcessor.of(new StructuredTableChunkPostProcessor()),
                observer
        );

        ParsedDocument parsed = new StructuredTableDocumentParser().parse(new DocumentSource(
                "memory://weekly.csv",
                "weekly.csv",
                "text/csv",
                new java.io.ByteArrayInputStream("""
                        Region,Q1,Q2
                        APAC,10,12
                        EMEA,8,9
                        """.getBytes(StandardCharsets.UTF_8)),
                Map.of()
        ));

        UUID traceId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        IngestionTraceContext traceContext = new IngestionTraceContext(
                traceId,
                runId,
                documentId,
                "memory://weekly.csv"
        );

        pipeline.prepareFromParsed(
                parsed,
                "memory://weekly.csv",
                UUID.randomUUID(),
                documentId,
                UUID.randomUUID(),
                libraryProfile(),
                tableProfile(),
                new ApproximateTokenizer("approx-test", "1"),
                PreparationStage.POST_PROCESS,
                Map.of(),
                traceContext
        );

        assertTrue(observer.stages().contains("normalize_text"));
        assertTrue(observer.stages().contains("extract_metadata"));
        assertTrue(observer.stages().contains("chunk_document"));
        assertTrue(observer.stages().contains("post_process_chunks"));
        assertEquals("SUCCEEDED", observer.lastStatus("post_process_chunks"));
    }

    @Test
    void documentSummaryPrepareStageRunsAfterChunkSpan() {
        RecordingPipelineObserver observer = new RecordingPipelineObserver();
        DocumentLlmSummaryGenerator summaryGenerator = new DocumentLlmSummaryGenerator(new com.knowbase.model.ChatModelClient() {
            @Override
            public String provider() {
                return "test";
            }

            @Override
            public String modelName() {
                return "summary-model";
            }

            @Override
            public com.knowbase.model.ChatCompletion complete(com.knowbase.model.ChatRequest request) {
                return new com.knowbase.model.ChatCompletion("Weekly report summary.", 4, 4, "");
            }
        });
        DocumentPreparationPipeline pipeline = new DocumentPreparationPipeline(
                new DocumentSourceLoader(null, List.of(new StructuredTableDocumentParser())),
                new DocumentTextNormalizer(),
                new TokenBasedDocumentChunker(
                        new com.knowbase.tokenizer.DefaultTokenizerRegistry(),
                        new com.knowbase.tokenizer.DefaultTokenWindowChunker()
                ),
                new DefaultDocumentMetadataEnricher(),
                CompositeChunkPostProcessor.of(new StructuredTableChunkPostProcessor()),
                summaryGenerator,
                observer
        );

        ParsedDocument parsed = new StructuredTableDocumentParser().parse(new DocumentSource(
                "memory://weekly.csv",
                "weekly.csv",
                "text/csv",
                new java.io.ByteArrayInputStream("""
                        Region,Q1,Q2
                        APAC,10,12
                        EMEA,8,9
                        """.getBytes(StandardCharsets.UTF_8)),
                Map.of()
        ));

        DocumentProfile profile = tableProfile();
        profile = new DocumentProfile(
                profile.documentProfileId(),
                profile.libraryId(),
                profile.code(),
                profile.contentFamily(),
                profile.parserCode(),
                profile.chunkingStrategy(),
                profile.tokenizerProfileId(),
                profile.metadataSchema(),
                Map.of("llmDocumentSummary", true, "llmSummaryMinInputChars", 10, "tableChunkPostProcess", true),
                profile.enabled()
        );

        DocumentPreparationResult result = pipeline.prepareFromParsed(
                parsed,
                "memory://weekly.csv",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                libraryProfile(),
                profile,
                new ApproximateTokenizer("approx-test", "1"),
                PreparationStage.DOCUMENT_SUMMARY,
                Map.of(),
                new IngestionTraceContext(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "memory://weekly.csv")
        );

        int summaryIndex = observer.stages().indexOf("summarize_document");
        int chunkIndex = observer.stages().indexOf("chunk_document");
        assertTrue(summaryIndex >= 0);
        assertTrue(chunkIndex >= 0);
        assertTrue(summaryIndex > chunkIndex);
        assertTrue(result.documentSummary().succeeded());
        assertEquals("SUCCEEDED", observer.lastStatus("summarize_document"));
    }

    @Test
    void chunkStageStopsBeforePostProcessSpan() {
        RecordingPipelineObserver observer = new RecordingPipelineObserver();
        DocumentPreparationPipeline pipeline = new DocumentPreparationPipeline(
                new DocumentSourceLoader(null, List.of(new StructuredTableDocumentParser())),
                new DocumentTextNormalizer(),
                new TokenBasedDocumentChunker(
                        new com.knowbase.tokenizer.DefaultTokenizerRegistry(),
                        new com.knowbase.tokenizer.DefaultTokenWindowChunker()
                ),
                new DefaultDocumentMetadataEnricher(),
                CompositeChunkPostProcessor.of(new StructuredTableChunkPostProcessor()),
                observer
        );

        ParsedDocument parsed = new StructuredTableDocumentParser().parse(new DocumentSource(
                "memory://weekly.csv",
                "weekly.csv",
                "text/csv",
                new java.io.ByteArrayInputStream("""
                        Region,Q1
                        APAC,10
                        """.getBytes(StandardCharsets.UTF_8)),
                Map.of()
        ));

        DocumentPreparationResult result = pipeline.prepareFromParsed(
                parsed,
                "memory://weekly.csv",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                libraryProfile(),
                tableProfile(),
                new ApproximateTokenizer("approx-test", "1"),
                PreparationStage.CHUNK,
                Map.of(),
                new IngestionTraceContext(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "memory://weekly.csv")
        );

        assertTrue(observer.stages().contains("chunk_document"));
        assertTrue(observer.stages().stream().noneMatch("post_process_chunks"::equals));
        assertTrue(observer.stages().stream().noneMatch("summarize_document"::equals));
        assertEquals(false, result.postProcess().applied());
    }

    private static DocumentProfile tableProfile() {
        return new DocumentProfile(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "default_table",
                ContentFamily.STRUCTURED_TABLE,
                "table-deep",
                "table_row_token_window",
                null,
                Map.of(),
                Map.of(
                        "tableChunkPostProcess", true,
                        "prependSheetContext", true
                ),
                true
        );
    }

    private static LibraryProfile libraryProfile() {
        return new LibraryProfile(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                "ollama",
                "bge-m3",
                1024,
                null,
                384,
                48,
                12,
                Map.of(),
                Instant.now()
        );
    }

    private static final class RecordingPipelineObserver implements PipelineObserver {
        private final List<String> stages = new CopyOnWriteArrayList<>();
        private final Map<UUID, String> spanStages = new ConcurrentHashMap<>();
        private final Map<String, String> stageStatuses = new ConcurrentHashMap<>();

        @Override
        public UUID startSpan(String pipeline, UUID runId, String stage, Map<String, Object> attributes) {
            UUID spanId = UUID.randomUUID();
            stages.add(stage);
            spanStages.put(spanId, stage);
            return spanId;
        }

        @Override
        public void finishSpan(UUID spanId, String status, Map<String, Object> attributes) {
            String stage = spanStages.remove(spanId);
            if (stage != null) {
                stageStatuses.put(stage, status);
            }
        }

        @Override
        public void recordIngestionError(UUID runId, String sourceUri, String errorCode, String message) {
        }

        List<String> stages() {
            return List.copyOf(stages);
        }

        String lastStatus(String stage) {
            return stageStatuses.get(stage);
        }
    }
}
