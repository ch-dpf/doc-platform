package com.knowbase.pipeline.content;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContentFamilyResolverTest {

    @Test
    void resolvesTabularSpreadsheetMime() {
        assertEquals(
                ContentFamily.TABULAR,
                ContentFamilyResolver.resolve(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    void resolvesDocumentPdf() {
        assertEquals(ContentFamily.DOCUMENT, ContentFamilyResolver.resolve("application/pdf"));
    }

    @Test
    void resolvesPlainMarkdownByExtension() {
        assertEquals(ContentFamily.PLAIN, ContentFamilyResolver.resolve("text/plain", "readme.md"));
    }

    @Test
    void resolvesImageMime() {
        assertEquals(ContentFamily.IMAGE, ContentFamilyResolver.resolve("image/png"));
    }
}
