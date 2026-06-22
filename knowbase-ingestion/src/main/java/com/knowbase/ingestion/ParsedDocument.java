package com.knowbase.ingestion;

import com.knowbase.domain.status.ContentFamily;

import java.util.List;
import java.util.Map;

public record ParsedDocument(
        String sourceUri,
        String title,
        String text,
        ContentFamily contentFamily,
        Map<String, Object> metadata,
        List<StructuralBlock> blocks
) {

    public ParsedDocument {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        blocks = blocks == null ? List.of() : List.copyOf(blocks);
    }

    public ParsedDocument(
            String sourceUri,
            String title,
            String text,
            ContentFamily contentFamily,
            Map<String, Object> metadata
    ) {
        this(sourceUri, title, text, contentFamily, metadata, List.of());
    }

    public boolean structureAware() {
        return !blocks.isEmpty();
    }
}
