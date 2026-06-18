package com.knowbase.ingestion;

import com.knowbase.domain.status.ContentFamily;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class OcrDocumentParser implements DocumentParser {

    private final Tika tika = new Tika();
    private final AutoDetectParser parser = new AutoDetectParser();

    @Override
    public boolean supports(String sourceUri, String mimeType) {
        String lowerMime = mimeType == null ? "" : mimeType.toLowerCase();
        if (lowerMime.startsWith("image/")) {
            return true;
        }
        String lowerUri = sourceUri == null ? "" : sourceUri.toLowerCase();
        return lowerUri.endsWith(".png")
                || lowerUri.endsWith(".jpg")
                || lowerUri.endsWith(".jpeg")
                || lowerUri.endsWith(".bmp")
                || lowerUri.endsWith(".webp")
                || lowerUri.endsWith(".tif")
                || lowerUri.endsWith(".tiff");
    }

    @Override
    public ParsedDocument parse(DocumentSource source) {
        Metadata metadata = new Metadata();
        if (source.filename() != null) {
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, source.filename());
        }
        if (source.mimeType() != null) {
            metadata.set(Metadata.CONTENT_TYPE, source.mimeType());
        }
        try {
            ParseContext context = new ParseContext();
            TesseractOCRConfig ocrConfig = new TesseractOCRConfig();
            ocrConfig.setSkipOcr(false);
            context.set(TesseractOCRConfig.class, ocrConfig);
            BodyContentHandler handler = new BodyContentHandler(-1);
            parser.parse(source.inputStream(), handler, metadata, context);
            String text = handler.toString();
            if (text == null || text.isBlank()) {
                text = tika.parseToString(source.inputStream(), metadata);
            }
            Map<String, Object> parsedMetadata = new HashMap<>(source.metadata());
            parsedMetadata.put("parser", "ocr");
            parsedMetadata.put("detectedContentType", metadata.get(Metadata.CONTENT_TYPE));
            parsedMetadata.put("ocrApplied", true);
            return new ParsedDocument(
                    source.sourceUri(),
                    firstNonBlank(metadata.get("title"), source.filename(), source.sourceUri()),
                    text,
                    ContentFamily.IMAGE_TEXT,
                    parsedMetadata
            );
        } catch (IOException | SAXException | TikaException exception) {
            throw new IllegalStateException("OCR 解析文档失败: " + source.sourceUri(), exception);
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "untitled";
    }
}
