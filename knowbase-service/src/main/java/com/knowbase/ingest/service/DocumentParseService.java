package com.knowbase.ingest.service;

import com.knowbase.ingest.config.OcrProperties;
import com.knowbase.ingest.parse.DocumentParseOptions;
import com.knowbase.ingest.parse.HtmlParsingContentProcessor;
import com.knowbase.ingest.parse.OcrFallbackPolicy;
import com.knowbase.ingest.parse.TikaMetadataHints;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.ToHTMLContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xml.sax.ContentHandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class DocumentParseService {

    private static final Logger log = LoggerFactory.getLogger(DocumentParseService.class);

    private final Tika tika = new Tika();
    private final AutoDetectParser parser = new AutoDetectParser();
    private final DocumentOcrService ocrService;
    private final OcrProperties ocrProperties;

    public DocumentParseService(DocumentOcrService ocrService, OcrProperties ocrProperties) {
        this.ocrService = ocrService;
        this.ocrProperties = ocrProperties;
    }

    public String extractText(InputStream inputStream, String fileName) {
        return extractText(readAll(inputStream), fileName, null, DocumentParseOptions.disabled());
    }

    public String extractText(byte[] bytes, String fileName, String mimeType, DocumentParseOptions options) {
        DocumentParseOptions effective = options != null ? options : DocumentParseOptions.disabled();
        String detectedMime = mimeType != null && !mimeType.isBlank()
                ? mimeType
                : detectMimeType(bytes, fileName);
        String tikaText = extractWithTika(bytes, fileName, effective);
        if (!effective.ocrEnabled()) {
            return tikaText;
        }
        if (!OcrFallbackPolicy.shouldFallback(
                tikaText, detectedMime, fileName, ocrProperties.getMinExtractedCharsToSkip())) {
            return tikaText;
        }
        if (!ocrService.isAvailable()) {
            throw new ParseException(
                    "知识库已开启 OCR，但服务端未启用 OCR 引擎（ingest.ocr.enabled=false 或 Tesseract 未就绪）",
                    null);
        }
        log.info("Applying OCR fallback for {} (tika chars={})", fileName, tikaText.length());
        String ocrText = ocrService.extract(bytes, detectedMime, fileName, effective.language());
        if (ocrText.isBlank() && !tikaText.isBlank()) {
            return tikaText;
        }
        return ocrText;
    }

    public String detectMimeType(byte[] sample, String fileName) {
        return tika.detect(sample, fileName);
    }

    private String extractWithTika(byte[] bytes, String fileName, DocumentParseOptions options) {
        DocumentParseOptions effective = options != null ? options : DocumentParseOptions.disabled();
        if (!effective.requiresHtmlPipeline()) {
            return extractPlainWithTika(bytes, fileName, effective);
        }
        try {
            String html = extractHtmlWithTika(bytes, fileName, effective);
            return HtmlParsingContentProcessor.apply(
                    html, effective, (imageBytes, mime) -> ocrService.tryOcrImage(imageBytes, mime, effective.language()));
        } catch (Exception e) {
            log.warn(
                    "HTML content extraction failed for {} (table={}, image={}, formula={}), falling back to plain text: {}",
                    fileName,
                    effective.tableExtraction(),
                    effective.imageExtraction(),
                    effective.formulaExtraction(),
                    e.getMessage());
            return extractPlainWithTika(bytes, fileName, effective);
        }
    }

    private String extractPlainWithTika(byte[] bytes, String fileName, DocumentParseOptions options) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
            ContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = buildMetadata(fileName, options);
            parser.parse(in, handler, metadata, new ParseContext());
            String text = handler.toString();
            if (text == null || text.isBlank()) {
                return "";
            }
            return text.trim();
        } catch (Exception e) {
            throw new ParseException("Failed to parse document: " + fileName, e);
        }
    }

    private String extractHtmlWithTika(byte[] bytes, String fileName, DocumentParseOptions options) throws Exception {
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes);
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ContentHandler handler = new ToHTMLContentHandler(out, StandardCharsets.UTF_8.name());
            Metadata metadata = buildMetadata(fileName, options);
            parser.parse(in, handler, metadata, new ParseContext());
            return out.toString(StandardCharsets.UTF_8);
        }
    }

    private Metadata buildMetadata(String fileName, DocumentParseOptions options) {
        Metadata metadata = new Metadata();
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
        TikaMetadataHints.apply(metadata, options);
        return metadata;
    }

    private static byte[] readAll(InputStream inputStream) {
        try {
            return inputStream.readAllBytes();
        } catch (Exception e) {
            throw new ParseException("Failed to read document stream", e);
        }
    }
}
