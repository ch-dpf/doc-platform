package com.knowbase.retrieval;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Canonical citation / evidence location fields copied from chunk metadata.
 */
public final class CitationLocationSupport {

    static final List<String> LOCATION_KEYS = List.of(
            "pageNumber",
            "bbox",
            "sheetName",
            "tableRegionId",
            "tableRegionLabel",
            "headerPath",
            "rowRole",
            "rowRange",
            "columnRange",
            "rowIndex",
            "cellCoordinates",
            "tableRegionBbox",
            "ocrConfidence",
            "ocrLanguage",
            "rotation",
            "lowConfidenceOcr",
            "reviewRequired",
            "ocrDownweightFactor",
            "ocrFilterReason",
            "layoutRole",
            "tableFormat",
            "contentFamily",
            "bboxSource",
            "confidenceSource",
            "evidenceAssetHint",
            "vectorScore",
            "keywordScore",
            "vectorRank",
            "keywordRank",
            "retrievalBackend"
    );

    private CitationLocationSupport() {
    }

    public static Map<String, Object> copyLocationFields(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> location = new HashMap<>();
        for (String key : LOCATION_KEYS) {
            copyIfPresent(metadata, location, key);
        }
        copyIfPresent(metadata, location, "sourceUri");
        copyIfPresent(metadata, location, "title");
        copyIfPresent(metadata, location, "documentId");
        copyIfPresent(metadata, location, "chunkId");
        copyIfPresent(metadata, location, "libraryId");
        return Map.copyOf(location);
    }

    private static void copyIfPresent(Map<String, Object> source, Map<String, Object> target, String key) {
        if (source.get(key) != null) {
            target.put(key, source.get(key));
        }
    }
}
