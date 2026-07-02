package com.knowbase.ingestion.ocr;

import java.util.List;

public record OcrWordResult(String text, List<Double> bbox, Double confidence) {
}
