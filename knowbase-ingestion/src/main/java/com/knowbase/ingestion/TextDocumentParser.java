package com.knowbase.ingestion;

import com.knowbase.domain.status.ContentFamily;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class TextDocumentParser implements DocumentParser {

    @Override
    public boolean supports(String sourceUri, String mimeType) {
        if (mimeType != null) {
            String lower = mimeType.toLowerCase();
            if (lower.contains("text") || lower.contains("markdown")) {
                return true;
            }
        }
        if (sourceUri == null) {
            return false;
        }
        String lower = sourceUri.toLowerCase();
        return lower.startsWith("inline:") || lower.endsWith(".md") || lower.endsWith(".txt") || lower.endsWith(".markdown");
    }

    @Override
    public ParsedDocument parse(DocumentSource source) {
        String text = readText(source);
        String title = source.filename() == null ? source.sourceUri() : source.filename();
        ContentFamily family = detectFamily(source);
        return new ParsedDocument(source.sourceUri(), title, text, family, source.metadata());
    }

    private static ContentFamily detectFamily(DocumentSource source) {
        String mimeType = source.mimeType();
        if (mimeType != null && mimeType.toLowerCase().contains("markdown")) {
            return ContentFamily.RICH_TEXT;
        }
        String uri = source.sourceUri() == null ? "" : source.sourceUri().toLowerCase();
        if (uri.endsWith(".md") || uri.endsWith(".markdown")) {
            return ContentFamily.RICH_TEXT;
        }
        return ContentFamily.PLAIN_TEXT;
    }

    private static String readText(DocumentSource source) {
        try (InputStream inputStream = source.inputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("读取文档失败: " + source.sourceUri(), exception);
        }
    }
}
