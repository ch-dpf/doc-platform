package com.knowbase.ingestion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExternalDocumentParserTest {

    @Test
    void extractsMarkdownTextFromJsonResponse() {
        String text = ExternalDocumentParser.extractText("{\"markdown\":\"# Title\\nParsed by Docling\"}");

        assertEquals("# Title\nParsed by Docling", text);
    }
}
