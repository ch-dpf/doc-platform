package com.knowbase.ingestion.metadata;

import com.knowbase.ingestion.document.ParsedDocument;

/**
 * Metadata extraction boundary used after cleaning and before chunking.
 */
public interface DocumentMetadataExtractor {

    ParsedDocument extract(ParsedDocument document, MetadataExtractionOptions options);

    default ParsedDocument extract(ParsedDocument document) {
        return extract(document, MetadataExtractionOptions.defaults());
    }

    record MetadataExtractionOptions(
            boolean includeBlockCounts,
            boolean includeTextStats,
            boolean includeFirstHeading
    ) {
        public static MetadataExtractionOptions defaults() {
            return new MetadataExtractionOptions(true, true, true);
        }
    }
}
