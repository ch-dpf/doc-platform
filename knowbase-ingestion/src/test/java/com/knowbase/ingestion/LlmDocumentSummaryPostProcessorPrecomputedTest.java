package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentChunk;
import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.domain.status.ContentFamily;
import com.knowbase.tokenizer.ApproximateTokenizer;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmDocumentSummaryPostProcessorPrecomputedTest {

    @Test
    void bindsPrecomputedSummaryWithoutCallingGenerator() {
        UUID libraryId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        DocumentProfile profile = new DocumentProfile(
                UUID.randomUUID(),
                libraryId,
                "default_text",
                ContentFamily.PLAIN_TEXT,
                "text-structure",
                "paragraph_token_window",
                null,
                Map.of(),
                Map.of("llmDocumentSummary", true),
                true
        );
        LibraryProfile libraryProfile = new LibraryProfile(
                UUID.randomUUID(),
                libraryId,
                1,
                "test",
                "test-model",
                768,
                null,
                512,
                64,
                8,
                Map.of(),
                Instant.now()
        );
        ParsedDocument document = new ParsedDocument(
                "memory://policy.txt",
                "Policy",
                "Retention policy body.",
                ContentFamily.PLAIN_TEXT,
                Map.of(),
                List.of()
        );
        ApproximateTokenizer tokenizer = new ApproximateTokenizer("approx-test", "1");
        DocumentChunk body = new DocumentChunk(
                UUID.randomUUID(),
                documentId,
                libraryId,
                UUID.randomUUID(),
                "Retention policy body.",
                4,
                tokenizer.tokenizerId(),
                tokenizer.tokenizerVersion(),
                "test-model",
                "paragraph",
                null,
                Map.of("chunkRole", "child", "indexable", true)
        );
        DocumentSummaryStageOutcome stageOutcome = DocumentSummaryStageOutcome.attempted(
                Optional.of(new DocumentLlmSummaryGenerator.LlmSummaryResult(
                        "Precomputed retention summary.",
                        "test",
                        "summary-model",
                        "generate_summary"
                )),
                120,
                "Retention policy body."
        );
        LlmDocumentSummaryPostProcessor processor = new LlmDocumentSummaryPostProcessor(
                new DocumentLlmSummaryGenerator(new com.knowbase.model.ChatModelClient() {
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
                        throw new IllegalStateException("LLM should not run when summary is precomputed");
                    }
                })
        );

        List<DocumentChunk> processed = processor.process(
                List.of(body),
                new ChunkPostProcessContext(
                        document,
                        libraryProfile,
                        profile,
                        tokenizer,
                        Map.of(),
                        stageOutcome
                )
        );

        DocumentChunk summary = processed.stream()
                .filter(chunk -> "document_summary".equals(chunk.chunkBoundaryType()))
                .findFirst()
                .orElseThrow();
        assertTrue(summary.content().contains("Precomputed retention summary."));
        assertEquals("llm", summary.metadata().get("summarySource"));
    }
}
