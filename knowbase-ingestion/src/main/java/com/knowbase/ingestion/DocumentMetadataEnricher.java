package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.model.LibraryProfile;

import java.util.Map;
import java.util.UUID;

public interface DocumentMetadataEnricher {

    ParsedDocument enrich(ParsedDocument document, MetadataContext context);

    record MetadataContext(
            String sourceUri,
            UUID libraryId,
            UUID documentId,
            UUID indexVersionId,
            LibraryProfile libraryProfile,
            DocumentProfile documentProfile,
            Map<String, Object> sourceOptions
    ) {
        public MetadataContext {
            sourceOptions = sourceOptions == null ? Map.of() : Map.copyOf(sourceOptions);
        }
    }
}
