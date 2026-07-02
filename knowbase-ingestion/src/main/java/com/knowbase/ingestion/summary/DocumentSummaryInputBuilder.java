package com.knowbase.ingestion.summary;

import com.knowbase.domain.model.DocumentChunk;
import com.knowbase.domain.status.ContentFamily;
import com.knowbase.ingestion.ParsedDocument;
import com.knowbase.ingestion.StructuralBlock;
import com.knowbase.ingestion.StructuredTableSummaryBuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds LLM input from post-chunk business text (WeKnora: sorted chunk contents), with ParsedDocument fallback.
 */
public final class DocumentSummaryInputBuilder {

    private DocumentSummaryInputBuilder() {
    }

    public static String build(
            ParsedDocument document,
            List<DocumentChunk> chunks,
            int maxInputChars
    ) {
        if (document != null
                && document.contentFamily() == ContentFamily.STRUCTURED_TABLE
                && document.blocks() != null
                && !document.blocks().isEmpty()) {
            String fromBlocks = buildFromParsedDocument(document, maxInputChars);
            if (!fromBlocks.isBlank()) {
                return fromBlocks;
            }
        }
        String fromChunks = buildFromChunks(chunks, maxInputChars);
        if (!fromChunks.isBlank()) {
            return fromChunks;
        }
        return buildFromParsedDocument(document, maxInputChars);
    }

    public static String buildFromChunks(List<DocumentChunk> chunks, int maxInputChars) {
        if (chunks == null || chunks.isEmpty()) {
            return "";
        }
        List<DocumentChunk> contentChunks = chunks.stream()
                .filter(DocumentSummaryInputBuilder::isSummaryInputChunk)
                .sorted(summaryChunkOrder())
                .toList();
        if (contentChunks.isEmpty()) {
            return "";
        }
        String joined = joinChunkContents(contentChunks);
        return LongContentSampler.sample(joined.trim(), maxInputChars);
    }

    public static String buildFromParsedDocument(ParsedDocument document, int maxInputChars) {
        if (document == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        if (document.contentFamily() == ContentFamily.STRUCTURED_TABLE) {
            builder.append(buildTableRowsFromBlocks(document));
        } else if (document.structureAware() && document.blocks() != null && !document.blocks().isEmpty()) {
            builder.append(joinBlockContents(document.blocks()));
        } else {
            builder.append(document.text() == null ? "" : document.text());
        }
        return LongContentSampler.sample(builder.toString().trim(), maxInputChars);
    }

    private static String buildTableRowsFromBlocks(ParsedDocument document) {
        Map<String, List<String>> rowsBySheet = new LinkedHashMap<>();
        for (StructuralBlock block : document.blocks()) {
            if (!"table_row".equals(block.blockType()) || block.content() == null || block.content().isBlank()) {
                continue;
            }
            String sheet = String.valueOf(block.metadata().getOrDefault("sheetName", "Table"));
            String rowText = StructuredTableSummaryBuilder.stripPrefixes(block.content().trim());
            rowsBySheet.computeIfAbsent(sheet, ignored -> new ArrayList<>()).add(rowText);
        }
        if (rowsBySheet.isEmpty()) {
            return document.text() == null ? "" : document.text();
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : rowsBySheet.entrySet()) {
            builder.append("## Sheet: ").append(entry.getKey()).append('\n');
            List<String> rows = entry.getValue();
            for (int index = 0; index < rows.size(); index++) {
                builder.append("Row ").append(index + 1).append(": ").append(rows.get(index)).append('\n');
            }
            builder.append('\n');
        }
        return builder.toString().trim();
    }

    private static String joinBlockContents(List<StructuralBlock> blocks) {
        List<String> segments = new ArrayList<>();
        for (StructuralBlock block : blocks) {
            if (block.content() == null || block.content().isBlank()) {
                continue;
            }
            if ("heading".equals(block.blockType())) {
                segments.add("# ".repeat(Math.max(1, block.level())) + block.content().trim());
            } else {
                segments.add(block.content().trim());
            }
        }
        return String.join("\n\n", segments);
    }

    private static String joinChunkContents(List<DocumentChunk> chunks) {
        StringBuilder builder = new StringBuilder();
        for (DocumentChunk chunk : chunks) {
            String text = normalizeChunkContent(chunk);
            if (text.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append("\n\n");
            }
            builder.append(text);
        }
        return builder.toString();
    }

    private static String normalizeChunkContent(DocumentChunk chunk) {
        String content = chunk.content() == null ? "" : chunk.content().trim();
        if (content.isBlank()) {
            return "";
        }
        return StructuredTableSummaryBuilder.stripPrefixes(content);
    }

    private static boolean isSummaryInputChunk(DocumentChunk chunk) {
        if (chunk == null || chunk.content() == null || chunk.content().isBlank()) {
            return false;
        }
        if ("document_summary".equals(chunk.chunkBoundaryType()) || "sheet_summary".equals(chunk.chunkBoundaryType())) {
            return false;
        }
        Map<String, Object> metadata = chunk.metadata() == null ? Map.of() : chunk.metadata();
        String role = String.valueOf(metadata.getOrDefault("chunkRole", ""));
        if ("document_summary".equals(role) || "table_summary".equals(role) || "sheet_summary".equals(role)) {
            return false;
        }
        if ("llm-document-summary".equals(metadata.get("chunkOptimization"))) {
            return false;
        }
        return true;
    }

    private static Comparator<DocumentChunk> summaryChunkOrder() {
        return Comparator
                .comparingInt(DocumentSummaryInputBuilder::sheetOrderKey)
                .thenComparingInt(DocumentSummaryInputBuilder::rowOrderKey)
                .thenComparingInt(DocumentSummaryInputBuilder::flatOrdinalKey);
    }

    private static int sheetOrderKey(DocumentChunk chunk) {
        Map<String, Object> metadata = chunk.metadata() == null ? Map.of() : chunk.metadata();
        Object sheetOrder = metadata.get("sheetOrder");
        if (sheetOrder instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    private static int rowOrderKey(DocumentChunk chunk) {
        Map<String, Object> metadata = chunk.metadata() == null ? Map.of() : chunk.metadata();
        for (String key : List.of("rowStart", "rowIndex", "rowEnd")) {
            Object value = metadata.get(key);
            if (value instanceof Number number) {
                return number.intValue();
            }
        }
        return Integer.MAX_VALUE;
    }

    private static int flatOrdinalKey(DocumentChunk chunk) {
        Map<String, Object> metadata = chunk.metadata() == null ? Map.of() : chunk.metadata();
        Object value = metadata.get("flatOrdinal");
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }
}
