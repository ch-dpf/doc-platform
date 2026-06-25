package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentChunk;
import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.tokenizer.ModelTokenizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Replaces or inserts a document_summary chunk using LLM output when enabled on the document profile.
 */
public final class LlmDocumentSummaryPostProcessor implements ChunkPostProcessor {

    private final DocumentLlmSummaryGenerator summaryGenerator;

    public LlmDocumentSummaryPostProcessor(DocumentLlmSummaryGenerator summaryGenerator) {
        this.summaryGenerator = Objects.requireNonNull(summaryGenerator, "summaryGenerator");
    }

    @Override
    public boolean supports(ChunkPostProcessContext context) {
        return readBoolean(context.documentProfile(), "llmDocumentSummary", false);
    }

    @Override
    public List<DocumentChunk> process(List<DocumentChunk> chunks, ChunkPostProcessContext context) {
        Objects.requireNonNull(chunks, "chunks");
        Objects.requireNonNull(context, "context");
        DocumentSummaryStageOutcome stageOutcome = context.documentSummary();
        if (stageOutcome != null && stageOutcome.llmResult() != null && stageOutcome.llmResult().isPresent()) {
            return upsertDocumentSummary(chunks, context, stageOutcome.llmResult().get());
        }
        if (stageOutcome != null && stageOutcome.attempted()) {
            return tagRuleBasedDocumentSummaries(chunks, context.documentProfile());
        }
        Optional<DocumentLlmSummaryGenerator.LlmSummaryResult> llmSummary = summaryGenerator.generate(context, chunks);
        if (llmSummary.isEmpty()) {
            return tagRuleBasedDocumentSummaries(chunks, context.documentProfile());
        }
        return upsertDocumentSummary(chunks, context, llmSummary.get());
    }

    private List<DocumentChunk> tagRuleBasedDocumentSummaries(
            List<DocumentChunk> chunks,
            DocumentProfile documentProfile
    ) {
        if (!readBoolean(documentProfile, "llmDocumentSummary", false)) {
            return chunks;
        }
        List<DocumentChunk> updated = new ArrayList<>(chunks.size());
        for (DocumentChunk chunk : chunks) {
            if (!isDocumentSummary(chunk)) {
                updated.add(chunk);
                continue;
            }
            Map<String, Object> metadata = new HashMap<>(chunk.metadata() == null ? Map.of() : chunk.metadata());
            metadata.putIfAbsent("summarySource", "rule");
            metadata.put("llmDocumentSummaryAttempted", true);
            updated.add(new DocumentChunk(
                    chunk.chunkId(),
                    chunk.documentId(),
                    chunk.libraryId(),
                    chunk.indexVersionId(),
                    chunk.content(),
                    chunk.tokenCount(),
                    chunk.tokenizerId(),
                    chunk.tokenizerVersion(),
                    chunk.embeddingModel(),
                    chunk.chunkBoundaryType(),
                    chunk.parentChunkId(),
                    Map.copyOf(metadata)
            ));
        }
        return updated;
    }

    private List<DocumentChunk> upsertDocumentSummary(
            List<DocumentChunk> chunks,
            ChunkPostProcessContext context,
            DocumentLlmSummaryGenerator.LlmSummaryResult llmSummary
    ) {
        DocumentChunk template = chunks.stream()
                .filter(chunk -> !isDocumentSummary(chunk))
                .findFirst()
                .orElse(chunks.getFirst());
        ModelTokenizer tokenizer = context.tokenizer();
        String content = formatSummaryContent(context.document(), llmSummary.summaryText());

        int existingIndex = -1;
        for (int index = 0; index < chunks.size(); index++) {
            if (isDocumentSummary(chunks.get(index))) {
                existingIndex = index;
                break;
            }
        }

        DocumentChunk summaryChunk = new DocumentChunk(
                existingIndex >= 0 ? chunks.get(existingIndex).chunkId() : UUID.randomUUID(),
                template.documentId(),
                template.libraryId(),
                template.indexVersionId(),
                content,
                tokenizer.count(content).tokens(),
                tokenizer.tokenizerId(),
                tokenizer.tokenizerVersion(),
                template.embeddingModel(),
                "document_summary",
                null,
                summaryMetadata(template, context.documentProfile(), llmSummary)
        );

        if (existingIndex >= 0) {
            List<DocumentChunk> updated = new ArrayList<>(chunks);
            updated.set(existingIndex, summaryChunk);
            return updated;
        }
        List<DocumentChunk> combined = new ArrayList<>(chunks.size() + 1);
        combined.add(summaryChunk);
        combined.addAll(chunks);
        return combined;
    }

    private static String formatSummaryContent(ParsedDocument document, String llmText) {
        String title = document == null || document.title() == null || document.title().isBlank()
                ? "Document"
                : document.title().trim();
        return ("Document summary: " + title + "\n" + llmText).trim();
    }

    private static Map<String, Object> summaryMetadata(
            DocumentChunk template,
            DocumentProfile documentProfile,
            DocumentLlmSummaryGenerator.LlmSummaryResult llmSummary
    ) {
        Map<String, Object> metadata = new HashMap<>();
        if (template.metadata() != null) {
            metadata.putAll(template.metadata());
        }
        metadata.put("chunkRole", "document_summary");
        metadata.put("indexable", true);
        metadata.put("chunkOptimization", "llm-document-summary");
        metadata.put("summarySource", "llm");
        metadata.put("summaryProvider", llmSummary.provider());
        metadata.put("summaryModel", llmSummary.model());
        metadata.put("summaryPromptId", llmSummary.promptId());
        if (documentProfile != null) {
            metadata.put("documentProfileCode", documentProfile.code());
        }
        return Map.copyOf(metadata);
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

    private static boolean readBoolean(DocumentProfile profile, String key, boolean defaultValue) {
        if (profile == null || profile.options() == null) {
            return defaultValue;
        }
        Object value = profile.options().get(key);
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value != null) {
            return Boolean.parseBoolean(String.valueOf(value).trim());
        }
        return defaultValue;
    }
}
