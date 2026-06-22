package com.knowbase.ingestion.parser;

import com.knowbase.ingestion.document.ParsedDocument;
import com.knowbase.ingestion.document.ParsedDocument.BlockType;
import com.knowbase.ingestion.document.ParsedDocument.CodeBlock;
import com.knowbase.ingestion.document.ParsedDocument.ContentFamily;
import com.knowbase.ingestion.document.ParsedDocument.TextBlock;
import com.knowbase.ingestion.parser.DocumentParser.ParseRequest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Minimal text and Markdown parser used before richer parser adapters are plugged in.
 */
public final class PlainTextDocumentParser implements DocumentParser {

    @Override
    public boolean supports(ParseRequest request) {
        Objects.requireNonNull(request, "request");
        String mediaType = request.mediaType();
        String sourceName = request.metadata().getOrDefault("sourceName", "").toLowerCase();
        return mediaType.equals("text/plain")
                || mediaType.equals("text/markdown")
                || mediaType.equals("text/x-markdown")
                || sourceName.endsWith(".txt")
                || sourceName.endsWith(".md")
                || sourceName.endsWith(".markdown");
    }

    @Override
    public ParsedDocument parse(ParseRequest request) {
        Objects.requireNonNull(request, "request");
        ContentFamily family = request.contentFamily() == null ? ContentFamily.PLAIN_TEXT : request.contentFamily();
        ParsedDocument.Builder builder = ParsedDocument.builder(request.documentId(), family)
                .metadata("parser", "plain-text")
                .metadata("mediaType", request.mediaType());
        request.metadata().forEach(builder::metadata);

        String[] lines = request.content().replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        StringBuilder paragraph = new StringBuilder();
        StringBuilder code = new StringBuilder();
        String codeLanguage = "";
        boolean insideCodeFence = false;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("```")) {
                if (insideCodeFence) {
                    flushParagraph(builder, paragraph);
                    if (!code.toString().isBlank()) {
                        builder.block(new CodeBlock(codeLanguage, code.toString().stripTrailing(), Map.of("source", "markdown-fence")));
                    }
                    code.setLength(0);
                    codeLanguage = "";
                    insideCodeFence = false;
                } else {
                    flushParagraph(builder, paragraph);
                    codeLanguage = trimmed.length() > 3 ? trimmed.substring(3).trim() : "";
                    insideCodeFence = true;
                }
                continue;
            }
            if (insideCodeFence) {
                code.append(line).append('\n');
                continue;
            }
            int headingLevel = markdownHeadingLevel(trimmed);
            if (headingLevel > 0) {
                flushParagraph(builder, paragraph);
                builder.block(new TextBlock(
                        BlockType.HEADING,
                        trimmed.substring(headingLevel).trim(),
                        ParsedDocument.metadata("level", String.valueOf(headingLevel))
                ));
                continue;
            }
            if (trimmed.isEmpty()) {
                flushParagraph(builder, paragraph);
            } else {
                if (!paragraph.isEmpty()) {
                    paragraph.append('\n');
                }
                paragraph.append(line);
            }
        }
        flushParagraph(builder, paragraph);
        if (insideCodeFence && !code.toString().isBlank()) {
            builder.block(new CodeBlock(codeLanguage, code.toString().stripTrailing(), Map.of("source", "markdown-fence-unclosed")));
        }
        return builder.build();
    }

    private static void flushParagraph(ParsedDocument.Builder builder, StringBuilder paragraph) {
        String text = paragraph.toString().trim();
        if (!text.isBlank()) {
            builder.block(new TextBlock(BlockType.PARAGRAPH, text, new LinkedHashMap<>()));
        }
        paragraph.setLength(0);
    }

    private static int markdownHeadingLevel(String line) {
        if (!line.startsWith("#")) {
            return 0;
        }
        int level = 0;
        while (level < line.length() && line.charAt(level) == '#') {
            level++;
        }
        if (level > 6 || level == line.length() || line.charAt(level) != ' ') {
            return 0;
        }
        return level;
    }
}
