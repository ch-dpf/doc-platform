package com.knowbase.ingestion.ocr;

import java.util.List;

public record OcrPageResult(int pageNumber, Double rotation, String language, List<OcrLineResult> lines) {
}
