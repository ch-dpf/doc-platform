package com.knowbase.ingestion.summary;

public record DocumentSummaryPromptTemplate(
        String id,
        String name,
        String description,
        boolean defaultTemplate,
        String content
) {
}
