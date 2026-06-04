package com.docplatform.ingest.support;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MimeTypeAllowlistTest {

    private static final List<String> ALLOWED = List.of("text/markdown", "text/plain");

    @Test
    void allowsWebMarkdownMime() {
        assertTrue(MimeTypeAllowlist.isAllowed("text/x-web-markdown", "README.md", ALLOWED));
    }

    @Test
    void allowsMarkdownByExtensionWhenPlainText() {
        assertTrue(MimeTypeAllowlist.isAllowed("text/plain", "readme.md", ALLOWED));
    }

    @Test
    void rejectsUnknownMimeForNonMarkdown() {
        assertFalse(MimeTypeAllowlist.isAllowed("application/x-unknown", "data.bin", ALLOWED));
    }
}
