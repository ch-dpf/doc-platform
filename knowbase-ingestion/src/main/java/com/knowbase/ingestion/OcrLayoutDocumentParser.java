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
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * OCR + 版面解析：对扫描 PDF / 图片执行 OCR，并按行/段落/表格启发式切分为结构块。
 */
public final class OcrLayoutDocumentParser implements DocumentParser {

    public static final String PARSER_CODE = "ocr-layout";

    private final Tika tika = new Tika();
    private final AutoDetectParser parser = new AutoDetectParser();

    @Override
    public boolean supports(String sourceUri, String mimeType) {
        String lowerMime = mimeType == null ? "" : mimeType.toLowerCase(Locale.ROOT);
        if (lowerMime.startsWith("image/")) {
            return true;
        }
        if (lowerMime.contains("pdf")) {
            return true;
        }
        String lowerUri = sourceUri == null ? "" : sourceUri.toLowerCase(Locale.ROOT);
        return lowerUri.endsWith(".png")
                || lowerUri.endsWith(".jpg")
                || lowerUri.endsWith(".jpeg")
                || lowerUri.endsWith(".bmp")
                || lowerUri.endsWith(".webp")
                || lowerUri.endsWith(".tif")
                || lowerUri.endsWith(".tiff")
                || lowerUri.endsWith(".pdf");
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
            byte[] content = source.inputStream().readAllBytes();
            ParseContext context = new ParseContext();
            TesseractOCRConfig ocrConfig = new TesseractOCRConfig();
            ocrConfig.setSkipOcr(false);
            String language = resolveLanguage(source.metadata());
            if (language != null && !language.isBlank()) {
                ocrConfig.setLanguage(language);
            }
            context.set(TesseractOCRConfig.class, ocrConfig);
            BodyContentHandler handler = new BodyContentHandler(-1);
            parser.parse(new java.io.ByteArrayInputStream(content), handler, metadata, context);
            String text = handler.toString();
            if (text == null || text.isBlank()) {
                text = tika.parseToString(new java.io.ByteArrayInputStream(content), metadata);
            }
            Map<String, Object> parsedMetadata = new HashMap<>();
            if (source.metadata() != null) {
                parsedMetadata.putAll(source.metadata());
            }
            List<StructuralBlock> blocks = StructureParsingSupport.parseOcrLayout(text, parsedMetadata);
            parsedMetadata.put("parserCode", PARSER_CODE);
            parsedMetadata.put("parser", PARSER_CODE);
            parsedMetadata.put("detectedContentType", metadata.get(Metadata.CONTENT_TYPE));
            parsedMetadata.put("ocrApplied", true);
            parsedMetadata.put("layoutParsing", true);
            parsedMetadata.put("ocrLanguage", language == null ? "auto" : language);
            parsedMetadata.putIfAbsent("ocrConfidence", -1d);
            parsedMetadata.putIfAbsent("ocrConfidenceSource", "unavailable");
            parsedMetadata.put("structureAware", !blocks.isEmpty());
            parsedMetadata.put("blockCount", blocks.size());
            String flatText = blocks.isEmpty() ? text : StructureParsingSupport.blocksToText(blocks);
            ContentFamily family = isPdf(source) ? ContentFamily.SCANNED_DOCUMENT : ContentFamily.IMAGE_TEXT;
            return new ParsedDocument(
                    source.sourceUri(),
                    firstNonBlank(metadata.get("title"), source.filename(), source.sourceUri()),
                    flatText,
                    family,
                    Map.copyOf(parsedMetadata),
                    blocks
            );
        } catch (IOException | SAXException | TikaException exception) {
            throw new IllegalStateException("OCR 版面解析失败: " + source.sourceUri(), exception);
        }
    }

    private static String resolveLanguage(Map<String, Object> metadata) {
        if (metadata == null) {
            return null;
        }
        Object language = metadata.get("ocrLanguage");
        if (language == null) {
            language = metadata.get("ocrLang");
        }
        return language == null ? null : String.valueOf(language);
    }

    private static boolean isPdf(DocumentSource source) {
        if (source.mimeType() != null && source.mimeType().toLowerCase(Locale.ROOT).contains("pdf")) {
            return true;
        }
        return source.sourceUri() != null && source.sourceUri().toLowerCase(Locale.ROOT).endsWith(".pdf");
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
