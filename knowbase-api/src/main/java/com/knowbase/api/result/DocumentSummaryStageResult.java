package com.knowbase.api.result;

/**
 * Document-level LLM summary stage preview; populated when prepareStage includes document_summary or later.
 */
public record DocumentSummaryStageResult(
        boolean enabled,
        boolean attempted,
        boolean succeeded,
        String summaryText,
        String provider,
        String model,
        String promptId,
        int inputCharCount,
        String inputPreview
) {
}
