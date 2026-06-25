package com.knowbase.ingestion.ocr;

import java.util.Map;

public interface OcrEngineAdapter {

    String engineCode();

    boolean supports(String mimeType, Map<String, Object> options);

    OcrEngineResult recognize(byte[] content, OcrRecognizeRequest request);
}
