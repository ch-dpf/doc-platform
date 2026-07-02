package com.knowbase.ingestion;

import java.util.List;
import java.util.Map;

/**
 * Backward-compatible entry for hOCR parsing; delegates to {@link com.knowbase.ingestion.ocr.OcrHocrParser}.
 */
final class OcrEngineOutputParser {

    private OcrEngineOutputParser() {
    }

    static List<StructuralBlock> parseHocrPublic(String hocr, Map<String, Object> metadata) {
        return com.knowbase.ingestion.ocr.OcrHocrParser.parse(hocr, metadata);
    }

    static List<StructuralBlock> parseHocr(String hocr, Map<String, Object> metadata) {
        return parseHocrPublic(hocr, metadata);
    }
}
