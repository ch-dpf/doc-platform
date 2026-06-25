package com.knowbase.retrieval;

import java.util.Map;

/**
 * Applies OCR confidence downweighting during retrieval fusion/rerank.
 */
public final class OcrRetrievalDownweightSupport {

    private OcrRetrievalDownweightSupport() {
    }

    public static double adjustedScore(double score, Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return score;
        }
        Object factor = metadata.get("ocrDownweightFactor");
        if (factor instanceof Number number) {
            return score * number.doubleValue();
        }
        if (Boolean.TRUE.equals(metadata.get("lowConfidenceOcr"))) {
            Object confidence = metadata.get("ocrConfidence");
            if (confidence instanceof Number confidenceNumber && confidenceNumber.doubleValue() >= 0) {
                return score * Math.max(0.1d, confidenceNumber.doubleValue());
            }
            return score * 0.5d;
        }
        return score;
    }
}
