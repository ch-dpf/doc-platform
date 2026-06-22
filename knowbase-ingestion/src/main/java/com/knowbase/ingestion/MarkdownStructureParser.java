package com.knowbase.ingestion;

import com.knowbase.domain.status.ContentFamily;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MarkdownStructureParser implements DocumentParser {

    public static final String PARSER_CODE = "markdown-structure";

    @Override
    public boolean supports(String sourceUri, String mimeType) {
        if (mimeType != null) {
            String lower = mimeType.toLowerCase();
            if (lower.contains("markdown")) {
                return true;
            }
        }
        if (sourceUri == null) {
            return false;
        }
        String lower = sourceUri.toLowerCase();
        return lower.endsWith(".md") || lower.endsWith(".markdown") || lower.startsWith("inline:text:");
    }

    @Override
    public ParsedDocument parse(DocumentSource source) {
        String text = readText(source);
        List<StructuralBlock> blocks = StructureParsingSupport.parseMarkdown(text);
        Map<String, Object> metadata = new HashMap<>();
        if (source.metadata() != null) {
            metadata.putAll(source.metadata());
        }
        metadata.put("parserCode", PARSER_CODE);
        metadata.put("structureAware", true);
        metadata.put("blockCount", blocks.size());
        String flatText = blocks.isEmpty() ? text : StructureParsingSupport.blocksToText(blocks);
        return new ParsedDocument(
                source.sourceUri(),
                source.filename(),
                flatText,
                ContentFamily.RICH_TEXT,
                Map.copyOf(metadata),
                blocks
        );
    }

    private static String readText(DocumentSource source) {
        try (InputStream inputStream = source.inputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("读取 Markdown 文档失败: " + source.sourceUri(), exception);
        }
    }
}
