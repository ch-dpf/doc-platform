package com.knowbase.ingestion.summary;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentSummaryPromptCatalogTest {

    @Test
    void loadsDefaultTemplateAndRendersLanguage() {
        DocumentSummaryPromptCatalog catalog = new DocumentSummaryPromptCatalog();
        String rendered = catalog.render("default_summary", Map.of("language", "Simplified Chinese"));
        assertTrue(rendered.contains("Simplified Chinese"));
        assertTrue(rendered.contains("3-5 key points"));
        assertTrue(!rendered.contains("{{language}}"));
    }
}
