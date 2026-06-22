package com.knowbase.ingestion;

import com.knowbase.domain.status.ContentFamily;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HtmlStructureParser implements DocumentParser {

    public static final String PARSER_CODE = "html-structure";

    @Override
    public boolean supports(String sourceUri, String mimeType) {
        if (mimeType != null) {
            String lower = mimeType.toLowerCase();
            if (lower.contains("html")) {
                return true;
            }
        }
        if (sourceUri == null) {
            return false;
        }
        String lower = sourceUri.toLowerCase();
        return lower.endsWith(".html") || lower.endsWith(".htm");
    }

    @Override
    public ParsedDocument parse(DocumentSource source) {
        String html = readText(source);
        List<StructuralBlock> blocks = StructureParsingSupport.parseHtml(html);
        Map<String, Object> metadata = new HashMap<>();
        if (source.metadata() != null) {
            metadata.putAll(source.metadata());
        }
        metadata.put("parserCode", PARSER_CODE);
        metadata.put("structureAware", true);
        metadata.put("blockCount", blocks.size());
        String flatText = blocks.isEmpty() ? html : StructureParsingSupport.blocksToText(blocks);
        return new ParsedDocument(
                source.sourceUri(),
                source.filename(),
                flatText,
                ContentFamily.WEB_PAGE,
                Map.copyOf(metadata),
                blocks
        );
    }

    private static String readText(DocumentSource source) {
        try (InputStream inputStream = source.inputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("读取 HTML 文档失败: " + source.sourceUri(), exception);
        }
    }
}
