package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentChunk;
import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.domain.status.ContentFamily;
import com.knowbase.model.ChatCompletion;
import com.knowbase.model.ChatModelClient;
import com.knowbase.model.ChatRequest;
import com.knowbase.tokenizer.ApproximateTokenizer;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmDocumentSummaryPostProcessorTest {

    @Test
    void replacesDocumentSummaryWithLlmOutput() {
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
                Map.of("llmDocumentSummary", true, "llmSummaryMinInputChars", 10),
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
                "This policy defines retention rules and access controls for customer data across regions.",
                ContentFamily.PLAIN_TEXT,
                Map.of(),
                List.of(StructuralBlock.paragraph(
                        "This policy defines retention rules and access controls for customer data across regions.",
                        0
                ))
        );
        ApproximateTokenizer tokenizer = new ApproximateTokenizer("approx-test", "1");
        DocumentChunk existing = new DocumentChunk(
                UUID.randomUUID(),
                documentId,
                libraryId,
                UUID.randomUUID(),
                "Document summary: Policy\nSheets indexed: 1",
                12,
                tokenizer.tokenizerId(),
                tokenizer.tokenizerVersion(),
                "test-model",
                "document_summary",
                null,
                Map.of("chunkRole", "document_summary", "summarySource", "rule")
        );
        DocumentChunk body = new DocumentChunk(
                UUID.randomUUID(),
                documentId,
                libraryId,
                existing.indexVersionId(),
                "This policy defines retention rules and access controls for customer data across regions.",
                4,
                tokenizer.tokenizerId(),
                tokenizer.tokenizerVersion(),
                "test-model",
                "paragraph",
                null,
                Map.of("chunkRole", "child", "indexable", true)
        );

        ChatModelClient chatModelClient = new ChatModelClient() {
            @Override
            public String provider() {
                return "test";
            }

            @Override
            public String modelName() {
                return "summary-model";
            }

            @Override
            public ChatCompletion complete(ChatRequest request) {
                return new ChatCompletion("Retention and access-control policy for customer data.", 10, 8, "");
            }
        };
        LlmDocumentSummaryPostProcessor processor = new LlmDocumentSummaryPostProcessor(
                new DocumentLlmSummaryGenerator(
                        chatModelClient,
                        new com.knowbase.ingestion.summary.DocumentSummaryPromptCatalog(),
                        com.knowbase.ingestion.summary.DocumentSummarySettings.defaults()
                )
        );
        List<DocumentChunk> processed = processor.process(
                List.of(existing, body),
                new ChunkPostProcessContext(document, libraryProfile, profile, tokenizer, Map.of())
        );

        DocumentChunk summary = processed.stream()
                .filter(chunk -> "document_summary".equals(chunk.chunkBoundaryType()))
                .findFirst()
                .orElseThrow();
        assertEquals(existing.chunkId(), summary.chunkId());
        assertTrue(summary.content().contains("Retention and access-control policy"));
        assertEquals("llm", summary.metadata().get("summarySource"));
        assertEquals("test", summary.metadata().get("summaryProvider"));
    }
}
