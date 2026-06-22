package com.knowbase.ingestion.cleaning;

import com.knowbase.ingestion.document.ParsedDocument;

/**
 * Cleaning boundary between parsing and chunking.
 */
public interface DocumentCleaner {

    ParsedDocument clean(ParsedDocument document, CleaningOptions options);

    default ParsedDocument clean(ParsedDocument document) {
        return clean(document, CleaningOptions.defaults());
    }

    record CleaningOptions(
            boolean collapseWhitespace,
            boolean collapseBlankLines,
            boolean trimCodeLines
    ) {
        public static CleaningOptions defaults() {
            return new CleaningOptions(true, true, true);
        }
    }
}
