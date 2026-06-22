package com.knowbase.ingestion;

import java.io.InputStream;
import java.util.Map;

public record DocumentSource(
        String sourceUri,
        String filename,
        String mimeType,
        InputStream inputStream,
        Map<String, Object> metadata
) {
}
