package com.knowbase.ingestion.cleaning;

import com.knowbase.ingestion.cleaning.DocumentCleaner.CleaningOptions;
import com.knowbase.ingestion.document.ParsedDocument;
import com.knowbase.ingestion.document.ParsedDocument.CodeBlock;
import com.knowbase.ingestion.document.ParsedDocument.DocumentBlock;
import com.knowbase.ingestion.document.ParsedDocument.FaqBlock;
import com.knowbase.ingestion.document.ParsedDocument.TableBlock;
import com.knowbase.ingestion.document.ParsedDocument.TableCell;
import com.knowbase.ingestion.document.ParsedDocument.TextBlock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Default cleaner that normalizes whitespace without changing document structure.
 */
public final class WhitespaceDocumentCleaner implements DocumentCleaner {

    @Override
    public ParsedDocument clean(ParsedDocument document, CleaningOptions options) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(options, "options");
        List<DocumentBlock> cleanedBlocks = new ArrayList<>();
        for (DocumentBlock block : document.blocks()) {
            DocumentBlock cleaned = cleanBlock(block, options);
            if (cleaned != null && !cleaned.asText().isBlank()) {
                cleanedBlocks.add(cleaned);
            }
        }
        Map<String, String> metadata = new LinkedHashMap<>(document.metadata());
        metadata.put("cleaner", "whitespace");
        metadata.put("cleaning.collapseWhitespace", String.valueOf(options.collapseWhitespace()));
        metadata.put("cleaning.collapseBlankLines", String.valueOf(options.collapseBlankLines()));
        return new ParsedDocument(document.documentId(), document.contentFamily(), cleanedBlocks, metadata);
    }

    private DocumentBlock cleanBlock(DocumentBlock block, CleaningOptions options) {
        if (block instanceof TextBlock text) {
            String value = normalizeText(text.text(), options);
            return value.isBlank() ? null : new TextBlock(text.type(), value, text.metadata());
        }
        if (block instanceof FaqBlock faq) {
            String question = normalizeText(faq.question(), options);
            String answer = normalizeText(faq.answer(), options);
            if (question.isBlank() || answer.isBlank()) {
                return null;
            }
            return new FaqBlock(
                    question,
                    answer,
                    faq.metadata()
            );
        }
        if (block instanceof CodeBlock code) {
            String value = normalizeCode(code.code(), options);
            return value.isBlank() ? null : new CodeBlock(code.language(), value, code.metadata());
        }
        if (block instanceof TableBlock table) {
            List<TableCell> cells = table.cells().stream()
                    .map(cell -> new TableCell(
                            normalizeText(cell.value(), options),
                            cell.rowIndex(),
                            cell.columnIndex(),
                            cell.rowSpan(),
                            cell.columnSpan(),
                            cell.header(),
                            cell.scope(),
                            cleanHeaders(cell.inheritedHeaders(), options),
                            cell.metadata()
                    ))
                    .toList();
            return new TableBlock(normalizeText(table.caption(), options), table.rowCount(), table.columnCount(), cells, table.metadata());
        }
        return block;
    }

    private Map<String, List<String>> cleanHeaders(Map<String, List<String>> headers, CleaningOptions options) {
        if (headers.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> cleaned = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            List<String> values = entry.getValue().stream()
                    .map(value -> normalizeText(value, options))
                    .filter(value -> !value.isBlank())
                    .toList();
            if (!values.isEmpty()) {
                cleaned.put(entry.getKey(), values);
            }
        }
        return cleaned;
    }

    private String normalizeText(String value, CleaningOptions options) {
        String normalized = value == null ? "" : value.trim();
        if (options.collapseWhitespace()) {
            normalized = normalized.replaceAll("[ \\t\\x0B\\f\\r]+", " ");
        }
        if (options.collapseBlankLines()) {
            normalized = normalized.replaceAll("\\n{3,}", "\n\n");
        }
        return normalized;
    }

    private String normalizeCode(String value, CleaningOptions options) {
        String normalized = value == null ? "" : value.replace("\r\n", "\n").replace('\r', '\n');
        if (!options.trimCodeLines()) {
            return normalized.stripTrailing();
        }
        StringBuilder builder = new StringBuilder();
        String[] lines = normalized.split("\n", -1);
        for (String line : lines) {
            builder.append(line.stripTrailing()).append('\n');
        }
        return builder.toString().stripTrailing();
    }
}
