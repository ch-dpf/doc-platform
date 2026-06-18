package com.knowbase.ingestion;

import com.knowbase.domain.status.ContentFamily;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class TikaDocumentParser implements DocumentParser {

    private final Tika tika = new Tika();

    @Override
    public boolean supports(String sourceUri, String mimeType) {
        String lowerMime = mimeType == null ? "" : mimeType.toLowerCase();
        if (lowerMime.contains("pdf")
                || lowerMime.contains("word")
                || lowerMime.contains("officedocument")
                || lowerMime.contains("opendocument")
                || lowerMime.contains("rtf")
                || lowerMime.contains("excel")
                || lowerMime.contains("spreadsheet")
                || lowerMime.contains("csv")
                || lowerMime.contains("powerpoint")
                || lowerMime.contains("presentation")
                || lowerMime.contains("html")
                || lowerMime.startsWith("image/")) {
            return true;
        }
        String lowerUri = sourceUri == null ? "" : sourceUri.toLowerCase();
        return lowerUri.endsWith(".pdf")
                || lowerUri.endsWith(".doc")
                || lowerUri.endsWith(".docx")
                || lowerUri.endsWith(".rtf")
                || lowerUri.endsWith(".odt")
                || lowerUri.endsWith(".xls")
                || lowerUri.endsWith(".xlsx")
                || lowerUri.endsWith(".ods")
                || lowerUri.endsWith(".csv")
                || lowerUri.endsWith(".ppt")
                || lowerUri.endsWith(".pptx")
                || lowerUri.endsWith(".odp")
                || lowerUri.endsWith(".html")
                || lowerUri.endsWith(".htm")
                || lowerUri.endsWith(".png")
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
            String text = tika.parseToString(source.inputStream(), metadata);
            String title = firstNonBlank(metadata.get("title"), source.filename(), source.sourceUri());
            Map<String, Object> parsedMetadata = new HashMap<>(source.metadata());
            parsedMetadata.put("parser", "tika");
            parsedMetadata.put("detectedContentType", metadata.get(Metadata.CONTENT_TYPE));
            parsedMetadata.put("resourceName", metadata.get(TikaCoreProperties.RESOURCE_NAME_KEY));
            return new ParsedDocument(source.sourceUri(), title, text, detectFamily(source), parsedMetadata);
        } catch (IOException | TikaException exception) {
            throw new IllegalStateException("Tika 解析文档失败: " + source.sourceUri(), exception);
        }
    }

    private static ContentFamily detectFamily(DocumentSource source) {
        String lower = source.sourceUri() == null ? "" : source.sourceUri().toLowerCase();
        if (lower.endsWith(".xls") || lower.endsWith(".xlsx")) {
            return ContentFamily.STRUCTURED_TABLE;
        }
        if (lower.endsWith(".csv") || lower.endsWith(".ods")) {
            return ContentFamily.STRUCTURED_TABLE;
        }
        if (lower.endsWith(".ppt") || lower.endsWith(".pptx")) {
            return ContentFamily.PRESENTATION;
        }
        if (lower.endsWith(".odp")) {
            return ContentFamily.PRESENTATION;
        }
        if (lower.endsWith(".html") || lower.endsWith(".htm")) {
            return ContentFamily.WEB_PAGE;
        }
        if (lower.endsWith(".png")
                || lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".bmp")
                || lower.endsWith(".webp")
                || lower.endsWith(".tif")
                || lower.endsWith(".tiff")) {
            return ContentFamily.IMAGE_TEXT;
        }
        return ContentFamily.RICH_TEXT;
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
