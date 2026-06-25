package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentChunk;
import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.ingestion.summary.DocumentSummaryInputBuilder;
import com.knowbase.ingestion.summary.DocumentSummaryPromptCatalog;
import com.knowbase.ingestion.summary.DocumentSummarySettings;
import com.knowbase.model.ChatModelClient;
import com.knowbase.model.ChatRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Document-level LLM summary: row/business chunk text + YAML prompt template + real chat model.
 */
public final class DocumentLlmSummaryGenerator {

    private final ChatModelClient chatModelClient;
    private final DocumentSummaryPromptCatalog promptCatalog;
    private final DocumentSummarySettings globalSettings;

    public DocumentLlmSummaryGenerator(ChatModelClient chatModelClient) {
        this(chatModelClient, new DocumentSummaryPromptCatalog(), DocumentSummarySettings.defaults());
    }

    public DocumentLlmSummaryGenerator(
            ChatModelClient chatModelClient,
            DocumentSummaryPromptCatalog promptCatalog,
            DocumentSummarySettings globalSettings
    ) {
        this.chatModelClient = Objects.requireNonNull(chatModelClient, "chatModelClient");
        this.promptCatalog = Objects.requireNonNull(promptCatalog, "promptCatalog");
        this.globalSettings = globalSettings == null ? DocumentSummarySettings.defaults() : globalSettings;
    }

    public DocumentSummaryStageOutcome generateStageOutcome(
            ChunkPostProcessContext context,
            List<DocumentChunk> chunks
    ) {
        Objects.requireNonNull(context, "context");
        DocumentProfile profile = context.documentProfile();
        if (profile == null || !readBoolean(profile, "llmDocumentSummary", false)) {
            return DocumentSummaryStageOutcome.disabled();
        }
        ParsedDocument document = context.document();
        if (document == null) {
            return DocumentSummaryStageOutcome.disabled();
        }
        DocumentSummarySettings settings = DocumentSummarySettings.merge(globalSettings, profile);
        String sampled = DocumentSummaryInputBuilder.build(document, chunks, settings.maxInputChars());
        int inputChars = sampled.length();
        String preview = truncatePreview(sampled, 240);
        if (inputChars < settings.minInputChars()) {
            return DocumentSummaryStageOutcome.skipped(inputChars, preview);
        }
        Optional<LlmSummaryResult> result = completeSummary(
                promptCatalog.render(settings.promptId(), Map.of("language", settings.language())),
                "Summarize the document content provided below.",
                sampled,
                settings
        );
        return DocumentSummaryStageOutcome.attempted(result, inputChars, preview);
    }

    public DocumentSummaryStageOutcome generateFromDocument(ParsedDocument document, DocumentProfile profile) {
        if (profile == null || !readBoolean(profile, "llmDocumentSummary", false)) {
            return DocumentSummaryStageOutcome.disabled();
        }
        if (document == null) {
            return DocumentSummaryStageOutcome.disabled();
        }
        DocumentSummarySettings settings = DocumentSummarySettings.merge(globalSettings, profile);
        String sampled = DocumentSummaryInputBuilder.build(document, List.of(), settings.maxInputChars());
        int inputChars = sampled.length();
        String preview = truncatePreview(sampled, 240);
        if (inputChars < settings.minInputChars()) {
            return DocumentSummaryStageOutcome.skipped(inputChars, preview);
        }
        Optional<LlmSummaryResult> result = completeSummary(
                promptCatalog.render(settings.promptId(), Map.of("language", settings.language())),
                "Summarize the document content provided below.",
                sampled,
                settings
        );
        return DocumentSummaryStageOutcome.attempted(result, inputChars, preview);
    }

    public Optional<LlmSummaryResult> generate(ChunkPostProcessContext context, List<DocumentChunk> chunks) {
        Objects.requireNonNull(context, "context");
        DocumentProfile profile = context.documentProfile();
        if (profile == null || !readBoolean(profile, "llmDocumentSummary", false)) {
            return Optional.empty();
        }
        ParsedDocument document = context.document();
        if (document == null) {
            return Optional.empty();
        }
        DocumentSummarySettings settings = DocumentSummarySettings.merge(globalSettings, profile);
        String sampled = DocumentSummaryInputBuilder.build(document, chunks, settings.maxInputChars());
        if (sampled.length() < settings.minInputChars()) {
            return Optional.empty();
        }
        String systemPrompt = promptCatalog.render(
                settings.promptId(),
                Map.of("language", settings.language())
        );
        String userMessage = "Summarize the document content provided below.";
        return completeSummary(systemPrompt, userMessage, sampled, settings);
    }

    private Optional<LlmSummaryResult> completeSummary(
            String systemPrompt,
            String userMessage,
            String contextText,
            DocumentSummarySettings settings
    ) {
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("purpose", "document_summary");
            parameters.put("temperature", settings.temperature());
            parameters.put("num_predict", settings.maxCompletionTokens());
            ChatRequest request = new ChatRequest(systemPrompt, userMessage, contextText, Map.copyOf(parameters));
            var completion = chatModelClient.complete(request);
            String answer = completion.answer();
            if (answer == null || answer.isBlank()) {
                return Optional.empty();
            }
            String trimmed = truncate(answer.trim(), settings.maxOutputChars());
            return Optional.of(new LlmSummaryResult(
                    trimmed,
                    chatModelClient.provider(),
                    chatModelClient.modelName(),
                    settings.promptId()
            ));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    static String sampleDocumentText(ParsedDocument document, int maxChars) {
        return DocumentSummaryInputBuilder.buildFromParsedDocument(document, maxChars);
    }

    private static String truncatePreview(String text, int maxChars) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return truncate(text.trim(), maxChars);
    }

    private static String truncate(String text, int maxChars) {
        if (text == null || text.length() <= maxChars) {
            return text == null ? "" : text;
        }
        return text.substring(0, Math.max(0, maxChars - 3)).trim() + "...";
    }

    private static boolean readBoolean(DocumentProfile profile, String key, boolean defaultValue) {
        if (profile.options() == null) {
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

    public record LlmSummaryResult(String summaryText, String provider, String model, String promptId) {
    }
}
