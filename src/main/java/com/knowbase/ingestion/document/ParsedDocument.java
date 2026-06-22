package com.knowbase.ingestion.document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Parsed document tree shared by parsers, normalizers and chunking strategies.
 */
public final class ParsedDocument {

    private final String documentId;
    private final ContentFamily contentFamily;
    private final List<DocumentBlock> blocks;
    private final Map<String, String> metadata;

    public ParsedDocument(
            String documentId,
            ContentFamily contentFamily,
            List<DocumentBlock> blocks,
            Map<String, String> metadata
    ) {
        this.documentId = requireText(documentId, "documentId");
        this.contentFamily = Objects.requireNonNull(contentFamily, "contentFamily");
        this.blocks = List.copyOf(Objects.requireNonNull(blocks, "blocks"));
        this.metadata = copyMetadata(metadata);
    }

    public String documentId() {
        return documentId;
    }

    public ContentFamily contentFamily() {
        return contentFamily;
    }

    public List<DocumentBlock> blocks() {
        return blocks;
    }

    public Map<String, String> metadata() {
        return metadata;
    }

    public String plainText() {
        StringJoiner joiner = new StringJoiner("\n\n");
        for (DocumentBlock block : blocks) {
            String text = block.asText().trim();
            if (!text.isEmpty()) {
                joiner.add(text);
            }
        }
        return joiner.toString();
    }

    public ParsedDocument withBlocks(List<DocumentBlock> blocks) {
        return new ParsedDocument(documentId, contentFamily, blocks, metadata);
    }

    public ParsedDocument withMetadata(Map<String, String> metadata) {
        return new ParsedDocument(documentId, contentFamily, blocks, metadata);
    }

    public ParsedDocument mergeMetadata(Map<String, String> extraMetadata) {
        if (extraMetadata == null || extraMetadata.isEmpty()) {
            return this;
        }
        Map<String, String> merged = new LinkedHashMap<>(metadata);
        merged.putAll(extraMetadata);
        return withMetadata(merged);
    }

    public static Builder builder(String documentId, ContentFamily contentFamily) {
        return new Builder(documentId, contentFamily);
    }

    public enum ContentFamily {
        PLAIN_TEXT,
        RICH_TEXT,
        STRUCTURED_TABLE,
        PRESENTATION,
        SCANNED_DOCUMENT,
        IMAGE_TEXT,
        WEB_PAGE,
        CODE_OR_CONFIG
    }

    public enum BlockType {
        HEADING,
        PARAGRAPH,
        TABLE,
        CODE,
        FAQ,
        PAGE_BREAK
    }

    public interface DocumentBlock {
        BlockType type();

        String asText();

        Map<String, String> metadata();
    }

    public record TextBlock(
            BlockType type,
            String text,
            Map<String, String> metadata
    ) implements DocumentBlock {

        public TextBlock {
            Objects.requireNonNull(type, "type");
            if (type != BlockType.HEADING && type != BlockType.PARAGRAPH && type != BlockType.PAGE_BREAK) {
                throw new IllegalArgumentException("TextBlock only supports heading, paragraph and page break types");
            }
            text = requireText(text, "text");
            metadata = copyMetadata(metadata);
        }

        @Override
        public String asText() {
            return text;
        }
    }

    public record CodeBlock(
            String language,
            String code,
            Map<String, String> metadata
    ) implements DocumentBlock {

        public CodeBlock {
            language = language == null ? "" : language.trim();
            code = requireText(code, "code");
            metadata = copyMetadata(metadata);
        }

        @Override
        public BlockType type() {
            return BlockType.CODE;
        }

        @Override
        public String asText() {
            return code;
        }
    }

    public record FaqBlock(
            String question,
            String answer,
            Map<String, String> metadata
    ) implements DocumentBlock {

        public FaqBlock {
            question = requireText(question, "question");
            answer = requireText(answer, "answer");
            metadata = copyMetadata(metadata);
        }

        @Override
        public BlockType type() {
            return BlockType.FAQ;
        }

        @Override
        public String asText() {
            return "Q: " + question + "\nA: " + answer;
        }
    }

    public record TableBlock(
            String caption,
            int rowCount,
            int columnCount,
            List<TableCell> cells,
            Map<String, String> metadata
    ) implements DocumentBlock {

        public TableBlock {
            caption = caption == null ? "" : caption.trim();
            if (rowCount < 0 || columnCount < 0) {
                throw new IllegalArgumentException("rowCount and columnCount must be non-negative");
            }
            cells = List.copyOf(Objects.requireNonNull(cells, "cells"));
            metadata = copyMetadata(metadata);
        }

        @Override
        public BlockType type() {
            return BlockType.TABLE;
        }

        @Override
        public String asText() {
            StringJoiner joiner = new StringJoiner("\n");
            String summary = summary();
            if (!summary.isEmpty()) {
                joiner.add(summary);
            }
            for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
                List<TableCell> row = row(rowIndex);
                if (!row.isEmpty()) {
                    StringJoiner rowText = new StringJoiner(" | ");
                    for (TableCell cell : row) {
                        rowText.add(cell.value());
                    }
                    joiner.add(rowText.toString());
                }
            }
            return joiner.toString();
        }

        public List<TableCell> row(int rowIndex) {
            List<TableCell> row = new ArrayList<>();
            for (TableCell cell : cells) {
                if (cell.rowIndex() == rowIndex) {
                    row.add(cell);
                }
            }
            row.sort((left, right) -> Integer.compare(left.columnIndex(), right.columnIndex()));
            return Collections.unmodifiableList(row);
        }

        public List<TableCell> dataCells() {
            List<TableCell> dataCells = new ArrayList<>();
            for (TableCell cell : cells) {
                if (!cell.header()) {
                    dataCells.add(cell);
                }
            }
            return Collections.unmodifiableList(dataCells);
        }

        public String summary() {
            StringBuilder builder = new StringBuilder("Table");
            if (!caption.isEmpty()) {
                builder.append(" \"").append(caption).append('"');
            }
            builder.append(": ").append(rowCount).append(" rows x ").append(columnCount).append(" columns");
            List<String> headers = cells.stream()
                    .filter(TableCell::header)
                    .map(TableCell::value)
                    .filter(value -> !value.isBlank())
                    .distinct()
                    .toList();
            if (!headers.isEmpty()) {
                builder.append("; headers: ").append(String.join(", ", headers));
            }
            return builder.toString();
        }
    }

    public record TableCell(
            String value,
            int rowIndex,
            int columnIndex,
            int rowSpan,
            int columnSpan,
            boolean header,
            String scope,
            Map<String, List<String>> inheritedHeaders,
            Map<String, String> metadata
    ) {

        public TableCell {
            value = value == null ? "" : value.trim().replaceAll("\\s+", " ");
            if (rowIndex < 0 || columnIndex < 0 || rowSpan < 1 || columnSpan < 1) {
                throw new IllegalArgumentException("table cell coordinates and spans must be positive");
            }
            scope = scope == null ? "" : scope.trim().toLowerCase();
            inheritedHeaders = copyHeaderMap(inheritedHeaders);
            metadata = copyMetadata(metadata);
        }

        public TableCell withInheritedHeaders(Map<String, List<String>> headers) {
            return new TableCell(value, rowIndex, columnIndex, rowSpan, columnSpan, header, scope, headers, metadata);
        }

        public boolean coversColumn(int column) {
            return columnIndex <= column && column < columnIndex + columnSpan;
        }

        public boolean coversRow(int row) {
            return rowIndex <= row && row < rowIndex + rowSpan;
        }
    }

    public static final class Builder {
        private final String documentId;
        private final ContentFamily contentFamily;
        private final List<DocumentBlock> blocks = new ArrayList<>();
        private final Map<String, String> metadata = new LinkedHashMap<>();

        private Builder(String documentId, ContentFamily contentFamily) {
            this.documentId = requireText(documentId, "documentId");
            this.contentFamily = Objects.requireNonNull(contentFamily, "contentFamily");
        }

        public Builder metadata(String key, String value) {
            if (key != null && value != null) {
                metadata.put(key, value);
            }
            return this;
        }

        public Builder block(DocumentBlock block) {
            blocks.add(Objects.requireNonNull(block, "block"));
            return this;
        }

        public ParsedDocument build() {
            return new ParsedDocument(documentId, contentFamily, blocks, metadata);
        }
    }

    public static Map<String, String> metadata(Object... entries) {
        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException("metadata entries must be key/value pairs");
        }
        Map<String, String> metadata = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            Object key = entries[i];
            Object value = entries[i + 1];
            if (key != null && value != null) {
                metadata.put(String.valueOf(key), String.valueOf(value));
            }
        }
        return copyMetadata(metadata);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }

    private static Map<String, String> copyMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }

    private static Map<String, List<String>> copyHeaderMap(Map<String, List<String>> headers) {
        if (headers == null || headers.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }
}
