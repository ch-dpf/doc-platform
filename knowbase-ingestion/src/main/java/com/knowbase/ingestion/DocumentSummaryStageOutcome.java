package com.knowbase.ingestion;

import java.util.Optional;

/**
 * Outcome of the document-level LLM summary stage ({@code summarize_document}), executed after chunking.
 */
public record DocumentSummaryStageOutcome(
        boolean enabled,
        boolean attempted,
        Optional<DocumentLlmSummaryGenerator.LlmSummaryResult> llmResult,
        String inputPreview,
        int inputCharCount
) {

    public static DocumentSummaryStageOutcome disabled() {
        return new DocumentSummaryStageOutcome(false, false, Optional.empty(), "", 0);
    }

    public static DocumentSummaryStageOutcome skipped(int inputCharCount, String inputPreview) {
        return new DocumentSummaryStageOutcome(true, false, Optional.empty(), inputPreview, inputCharCount);
    }

    public static DocumentSummaryStageOutcome attempted(
            Optional<DocumentLlmSummaryGenerator.LlmSummaryResult> llmResult,
            int inputCharCount,
            String inputPreview
    ) {
        return new DocumentSummaryStageOutcome(true, true, llmResult, inputPreview, inputCharCount);
    }

    public boolean succeeded() {
        return llmResult != null && llmResult.isPresent();
    }
}
