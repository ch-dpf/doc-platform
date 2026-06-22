package com.knowbase.ingestion;

import com.knowbase.domain.status.ContentFamily;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class ParsedDocumentStructureEnricher {

    private ParsedDocumentStructureEnricher() {
    }

    static ParsedDocument enrich(ParsedDocument parsed, String sourceUri) {
        if (parsed.structureAware()) {
            return parsed;
        }
        String text = parsed.text();
        if (text == null || text.isBlank()) {
            return parsed;
        }

        List<StructuralBlock> blocks = detectBlocks(text, sourceUri, parsed.contentFamily());
        if (blocks.isEmpty()) {
            return parsed;
        }

        Map<String, Object> metadata = new HashMap<>();
        if (parsed.metadata() != null) {
            metadata.putAll(parsed.metadata());
        }
        metadata.put("structureEnriched", true);
        metadata.put("structureAware", true);
        metadata.put("blockCount", blocks.size());

        return new ParsedDocument(
                parsed.sourceUri(),
                parsed.title(),
                StructureParsingSupport.blocksToText(blocks),
                parsed.contentFamily(),
                Map.copyOf(metadata),
                blocks
        );
    }

    private static List<StructuralBlock> detectBlocks(String text, String sourceUri, ContentFamily family) {
        String lowerUri = sourceUri == null ? "" : sourceUri.toLowerCase(Locale.ROOT);
        if (lowerUri.endsWith(".md") || lowerUri.endsWith(".markdown") || looksLikeMarkdown(text)) {
            return StructureParsingSupport.parseMarkdown(text);
        }
        if (family == ContentFamily.WEB_PAGE || lowerUri.endsWith(".html") || lowerUri.endsWith(".htm") || looksLikeHtml(text)) {
            return StructureParsingSupport.parseHtmlWithJsoup(text);
        }
        return StructureParsingSupport.parsePlainText(text);
    }

    private static boolean looksLikeMarkdown(String text) {
        return text.lines().anyMatch(line -> line.trim().matches("^#{1,6}\\s+\\S+.*"))
                || text.contains("```");
    }

    private static boolean looksLikeHtml(String text) {
        String trimmed = text.trim().toLowerCase(Locale.ROOT);
        return trimmed.startsWith("<!doctype html") || trimmed.startsWith("<html") || trimmed.contains("<body");
    }
}
