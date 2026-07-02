package com.knowbase.ingestion.parse;

import java.util.HashMap;
import java.util.Map;

/**
 * Copies OCR confidence metadata from structural blocks into chunk metadata for retrieval downweighting.
 */
public final class OcrChunkMetadataSupport {

    private static final String[] OCR_CHUNK_KEYS = {
            "ocrApplied",
            "ocrConfidence",
            "ocrDownweightFactor",
            "lowConfidenceOcr",
            "reviewRequired",
            "ocrFilterReason",
            "ocrEngine",
            "ocrLanguage",
            "layoutProvider"
    };

    private OcrChunkMetadataSupport() {
    }

    public static Map<String, Object> mergeBlockOcrFields(Map<String, Object> chunkMetadata, Map<String, Object> blockMetadata) {
        if (blockMetadata == null || blockMetadata.isEmpty()) {
            return chunkMetadata;
        }
        Map<String, Object> merged = chunkMetadata == null ? new HashMap<>() : new HashMap<>(chunkMetadata);
        for (String key : OCR_CHUNK_KEYS) {
            if (blockMetadata.containsKey(key)) {
                merged.put(key, blockMetadata.get(key));
            }
        }
        if (Boolean.TRUE.equals(merged.get("lowConfidenceOcr"))) {
            merged.putIfAbsent("indexable", !Boolean.FALSE.equals(blockMetadata.get("indexableHint")));
        }
        return Map.copyOf(merged);
    }
}
