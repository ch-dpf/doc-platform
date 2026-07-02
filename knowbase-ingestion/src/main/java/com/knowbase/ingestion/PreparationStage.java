package com.knowbase.ingestion;

import java.util.Locale;

public enum PreparationStage {
    PARSE,
    NORMALIZE,
    DOCUMENT_SUMMARY,
    CHUNK,
    POST_PROCESS,
    ALL;

    public static PreparationStage from(String value) {
        if (value == null || value.isBlank()) {
            return ALL;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "parse" -> PARSE;
            case "normalize" -> NORMALIZE;
            case "document_summary", "document-summary", "summarize", "summary" -> DOCUMENT_SUMMARY;
            case "chunk" -> CHUNK;
            case "post_process", "postprocess", "post-process" -> POST_PROCESS;
            case "all", "full" -> ALL;
            default -> throw new IllegalArgumentException("不支持的 prepareStage: " + value);
        };
    }

    public PreparationStage executionStage() {
        return this == ALL ? CHUNK : this;
    }

    public boolean runsDocumentSummary() {
        return executionStage() == DOCUMENT_SUMMARY;
    }

    public boolean runsPostProcess() {
        PreparationStage executionStage = executionStage();
        return executionStage == POST_PROCESS;
    }
}
