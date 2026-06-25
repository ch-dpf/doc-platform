package com.knowbase.ingestion.ocr;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TesseractOcrEngineAdapter implements OcrEngineAdapter {

    private final AutoDetectParser parser = new AutoDetectParser();

    @Override
    public String engineCode() {
        return "tesseract";
    }

    @Override
    public boolean supports(String mimeType, Map<String, Object> options) {
        return true;
    }

    @Override
    public OcrEngineResult recognize(byte[] content, OcrRecognizeRequest request) {
        Metadata metadata = new Metadata();
        if (request.mimeType() != null) {
            metadata.set(Metadata.CONTENT_TYPE, request.mimeType());
        }
        try {
            ParseContext context = new ParseContext();
            TesseractOCRConfig ocrConfig = new TesseractOCRConfig();
            ocrConfig.setSkipOcr(false);
            if (request.language() != null && !request.language().isBlank()) {
                ocrConfig.setLanguage(request.language());
            }
            context.set(TesseractOCRConfig.class, ocrConfig);
            BodyContentHandler handler = new BodyContentHandler(-1);
            parser.parse(new java.io.ByteArrayInputStream(content), handler, metadata, context);
            Map<String, Object> engineMetadata = new HashMap<>();
            for (String name : metadata.names()) {
                engineMetadata.put(name, metadata.get(name));
            }
            Object contentLanguage = metadata.get(Metadata.CONTENT_LANGUAGE);
            if (contentLanguage != null && !String.valueOf(contentLanguage).isBlank()) {
                engineMetadata.putIfAbsent("ocrLanguage", String.valueOf(contentLanguage).trim());
            }
            Object orientation = metadata.get("tesseract-ocr:orientation");
            if (orientation == null) {
                orientation = metadata.get("Orientation");
            }
            if (orientation != null) {
                engineMetadata.putIfAbsent("rotation", orientation);
                engineMetadata.putIfAbsent("pageRotation", orientation);
            }
            Object hocr = engineMetadata.get("ocr_hocr");
            if (hocr == null) {
                hocr = engineMetadata.get("X-OCR-THOCR");
            }
            if (hocr != null && !String.valueOf(hocr).isBlank()) {
                return new OcrEngineResult(
                        engineCode(),
                        "hocr",
                        String.valueOf(hocr),
                        List.of(),
                        engineMetadata
                );
            }
            Object tsv = engineMetadata.get("ocr_tsv");
            if (tsv != null && !String.valueOf(tsv).isBlank()) {
                return new OcrEngineResult(engineCode(), "tsv", String.valueOf(tsv), List.of(), engineMetadata);
            }
            return new OcrEngineResult(
                    engineCode(),
                    "text",
                    handler.toString(),
                    List.of(),
                    engineMetadata
            );
        } catch (IOException | SAXException | TikaException exception) {
            throw new IllegalStateException("Tesseract OCR failed: " + request.sourceUri(), exception);
        }
    }
}
