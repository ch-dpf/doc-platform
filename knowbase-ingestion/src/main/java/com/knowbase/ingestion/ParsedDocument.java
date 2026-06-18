package com.knowbase.ingestion;

import com.knowbase.domain.status.ContentFamily;

import java.util.Map;

public record ParsedDocument(
        String sourceUri,
        String title,
        String text,
        ContentFamily contentFamily,
        Map<String, Object> metadata
) {
}
