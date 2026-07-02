package com.knowbase.ingestion.ocr;

import java.util.List;

public record OcrLineResult(
        String text,
        List<Double> bbox,
        Double confidence,
        List<OcrWordResult> words,
        String level,
        String language,
        Double rotation
) {
    public OcrLineResult(String text, List<Double> bbox, Double confidence, List<OcrWordResult> words, String level) {
        this(text, bbox, confidence, words, level, null, null);
    }
}
