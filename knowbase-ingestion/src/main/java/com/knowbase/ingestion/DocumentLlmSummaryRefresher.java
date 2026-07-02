package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentChunk;
import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.model.IndexedChunk;
import com.knowbase.domain.model.KnowledgeDocument;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.domain.observability.PipelineObserver;
import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.model.EmbeddingModelClient;
import com.knowbase.model.ollama.OllamaEmbeddingModelClient;
import com.knowbase.tokenizer.ModelTokenizer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * Applies document-level LLM summary after chunking and initial indexing (async on the main pipeline).
 */
public final class DocumentLlmSummaryRefresher {

    private final KnowbaseRepository repository;
    private final DocumentLlmSummaryGenerator summaryGenerator;
    private final LlmDocumentSummaryPostProcessor summaryPostProcessor;
    private final EmbeddingModelClient embeddingModelClient;
    private final PipelineObserver pipelineObserver;

    public DocumentLlmSummaryRefresher(
            KnowbaseRepository repository,
            DocumentLlmSummaryGenerator summaryGenerator,
            LlmDocumentSummaryPostProcessor summaryPostProcessor,
            EmbeddingModelClient embeddingModelClient,
            PipelineObserver pipelineObserver
    ) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.summaryGenerator = Objects.requireNonNull(summaryGenerator, "summaryGenerator");
        this.summaryPostProcessor = Objects.requireNonNull(summaryPostProcessor, "summaryPostProcessor");
        this.embeddingModelClient = Objects.requireNonNull(embeddingModelClient, "embeddingModelClient");
        this.pipelineObserver = pipelineObserver == null
                ? new com.knowbase.domain.observability.NoopPipelineObserver()
                : pipelineObserver;
    }

    public void scheduleAfterIndex(Executor executor, DocumentLlmSummaryRefreshRequest request) {
        Objects.requireNonNull(executor, "executor");
        Objects.requireNonNull(request, "request");
        executor.execute(() -> applyAfterIndex(request));
    }

    public void applyAfterIndex(DocumentLlmSummaryRefreshRequest request) {
        Objects.requireNonNull(request, "request");
        if (!llmSummaryEnabled(request.documentProfile())) {
            return;
        }
        UUID spanId = pipelineObserver.startSpan(
                "ingestion",
                request.runId(),
                "summarize_document",
                spanAttributes(request)
        );
        try {
            List<DocumentChunk> chunks = repository.listChunksByDocument(request.documentId());
            if (chunks.isEmpty()) {
                pipelineObserver.finishSpan(spanId, "SKIPPED", Map.of("reason", "no_chunks"));
                return;
            }
            ChunkPostProcessContext context = new ChunkPostProcessContext(
                    request.parsedDocument(),
                    request.libraryProfile(),
                    request.documentProfile(),
                    request.tokenizer(),
                    request.sourceOptions()
            );
            DocumentSummaryStageOutcome outcome = summaryGenerator.generateStageOutcome(context, chunks);
            if (!outcome.succeeded()) {
                pipelineObserver.finishSpan(spanId, outcome.attempted() ? "SKIPPED" : "DISABLED", Map.of(
                        "attempted", outcome.attempted(),
                        "inputCharCount", outcome.inputCharCount()
                ));
                return;
            }
            List<DocumentChunk> updated = summaryPostProcessor.process(
                    chunks,
                    new ChunkPostProcessContext(
                            request.parsedDocument(),
                            request.libraryProfile(),
                            request.documentProfile(),
                            request.tokenizer(),
                            request.sourceOptions(),
                            outcome
                    )
            );
            DocumentChunk summaryChunk = findDocumentSummary(updated).orElse(null);
            if (summaryChunk == null) {
                pipelineObserver.finishSpan(spanId, "FAILED", Map.of("error", "summary_chunk_missing"));
                return;
            }
            float[] embedding = embedSingle(request.libraryProfile(), summaryChunk.content());
            repository.updateIndexedChunk(new IndexedChunk(summaryChunk, embedding));
            repository.findDocument(request.documentId())
                    .map(KnowledgeDocument::indexVersionId)
                    .ifPresent(repository::refreshIndexVersionStats);
            pipelineObserver.finishSpan(spanId, "SUCCEEDED", Map.of(
                    "summaryChars", summaryChunk.content().length(),
                    "inputCharCount", outcome.inputCharCount()
            ));
        } catch (RuntimeException exception) {
            pipelineObserver.finishSpan(spanId, "FAILED", Map.of(
                    "error", exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage()
            ));
        }
    }

    private float[] embedSingle(LibraryProfile profile, String content) {
        if (embeddingModelClient instanceof OllamaEmbeddingModelClient ollamaClient) {
            String model = profile.embeddingModel() == null || profile.embeddingModel().isBlank()
                    ? embeddingModelClient.modelName()
                    : profile.embeddingModel();
            return ollamaClient.embed(model, List.of(content)).getFirst();
        }
        return embeddingModelClient.embed(List.of(content)).getFirst();
    }

    private static Optional<DocumentChunk> findDocumentSummary(List<DocumentChunk> chunks) {
        return chunks.stream()
                .filter(DocumentLlmSummaryRefresher::isDocumentSummary)
                .findFirst();
    }

    private static boolean isDocumentSummary(DocumentChunk chunk) {
        if ("document_summary".equals(chunk.chunkBoundaryType())) {
            return true;
        }
        if (chunk.metadata() == null) {
            return false;
        }
        return "document_summary".equals(String.valueOf(chunk.metadata().get("chunkRole")));
    }

    private static boolean llmSummaryEnabled(DocumentProfile profile) {
        if (profile == null || profile.options() == null) {
            return false;
        }
        Object value = profile.options().get("llmDocumentSummary");
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value).trim());
    }

    private static Map<String, Object> spanAttributes(DocumentLlmSummaryRefreshRequest request) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("documentId", request.documentId().toString());
        attributes.put("sourceUri", request.sourceUri());
        if (request.traceId() != null) {
            attributes.put("traceId", request.traceId().toString());
        }
        return Map.copyOf(attributes);
    }

    public record DocumentLlmSummaryRefreshRequest(
            UUID traceId,
            UUID runId,
            UUID documentId,
            String sourceUri,
            ParsedDocument parsedDocument,
            LibraryProfile libraryProfile,
            DocumentProfile documentProfile,
            ModelTokenizer tokenizer,
            Map<String, Object> sourceOptions
    ) {
    }
}
