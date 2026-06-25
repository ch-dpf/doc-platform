package com.knowbase.model.vision;

import java.util.Map;

public final class VisionDocumentPrompts {

    public static final String DEFAULT_PAGE_PROMPT = """
            Extract all text from this document page. Preserve natural reading order.
            Use markdown: # for main headings, ## for subheadings.
            Separate paragraphs with blank lines.
            For tables, output pipe-separated rows, e.g. col1 | col2 | col3.
            Output only the extracted document content with no commentary or preamble.
            """;

    private VisionDocumentPrompts() {
    }

    public static String resolvePagePrompt(Map<String, Object> options) {
        if (options == null) {
            return DEFAULT_PAGE_PROMPT;
        }
        Object custom = options.get("visionLanguagePrompt");
        if (custom == null) {
            custom = options.get("vlPrompt");
        }
        if (custom == null || String.valueOf(custom).isBlank()) {
            return DEFAULT_PAGE_PROMPT;
        }
        return String.valueOf(custom).trim();
    }
}
