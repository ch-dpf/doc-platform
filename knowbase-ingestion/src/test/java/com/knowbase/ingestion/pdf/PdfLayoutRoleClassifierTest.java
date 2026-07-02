package com.knowbase.ingestion.pdf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfLayoutRoleClassifierTest {

    @Test
    void classifiesFooterNearBottom() {
        assertEquals("footer", PdfLayoutRoleClassifier.classify("1", 24f, 800f, 12f, 12f));
        assertEquals("footer", PdfLayoutRoleClassifier.classify("Page 2", 20f, 800f, 12f, 12f));
    }

    @Test
    void classifiesHeadingByFontSize() {
        assertEquals("title", PdfLayoutRoleClassifier.classify("Overview", 680f, 800f, 20f, 12f));
        assertEquals("heading", PdfLayoutRoleClassifier.classify("Section A", 640f, 800f, 15f, 12f));
    }

    @Test
    void classifiesPageHeaderSeparatelyFromFooter() {
        assertEquals("header", PdfLayoutRoleClassifier.classify("Quarterly Report", 760f, 800f, 12f, 12f));
    }

    @Test
    void headerIsNotIndexable() {
        assertFalse(PdfLayoutRoleClassifier.isIndexableRole("header"));
        assertFalse(PdfLayoutRoleClassifier.isIndexableRole("footer"));
        assertTrue(PdfLayoutRoleClassifier.isIndexableRole("body"));
    }
}
