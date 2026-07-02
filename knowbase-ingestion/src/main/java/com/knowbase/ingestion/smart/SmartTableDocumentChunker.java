package com.knowbase.ingestion.smart;

import com.knowbase.domain.model.DocumentChunk;
import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.domain.status.ContentFamily;
import com.knowbase.ingestion.ParsedDocument;
import com.knowbase.ingestion.StructuralBlock;
import com.knowbase.ingestion.StructuredTableSummaryBuilder;
import com.knowbase.ingestion.TableChunkConfig;
import com.knowbase.ingestion.TableRowIndexability;
import com.knowbase.tokenizer.ModelTokenizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Table chunking driven by L1/L2 {@link TableChunkConfig}:
 * {@code table_row} = one row per chunk; {@code table_row_token_window} = merge rows to L1 token budget.
 */
public final class SmartTableDocumentChunker {

    public List<DocumentChunk> chunk(
            UUID libraryId,
            UUID documentId,
            UUID indexVersionId,
            ParsedDocument document,
            LibraryProfile profile,
            DocumentProfile documentProfile,
            ModelTokenizer tokenizer,
            Map<String, Object> requestOptions
    ) {
        Map<String, List<StructuralBlock>> rowsBySheet = groupRowsBySheet(document);
        if (rowsBySheet.isEmpty()) {
            return List.of();
        }
        TableChunkConfig config = TableChunkConfig.resolve(profile, documentProfile, requestOptions);
        List<DocumentChunk> chunks = new ArrayList<>();
        for (Map.Entry<String, List<StructuralBlock>> entry : rowsBySheet.entrySet()) {
            int sheetColumnCount = sheetColumnCount(entry.getValue());
            emitSheet(
                    libraryId,
                    documentId,
                    indexVersionId,
                    document,
                    documentProfile,
                    profile,
                    tokenizer,
                    chunks,
                    entry.getKey(),
                    entry.getValue(),
                    config,
                    sheetColumnCount
            );
        }
        return chunks;
    }

    public static boolean shouldUse(
            ParsedDocument document,
            DocumentProfile documentProfile,
            Map<String, Object> requestOptions
    ) {
        if (document == null || documentProfile == null) {
            return false;
        }
        TableChunkConfig config = TableChunkConfig.resolve(null, documentProfile, requestOptions);
        if (!"smart".equalsIgnoreCase(config.chunkEngine())) {
            return false;
        }
        if (document.contentFamily() == ContentFamily.STRUCTURED_TABLE) {
            return true;
        }
        String strategy = config.chunkingStrategy();
        if (strategy == null || strategy.isBlank()) {
            strategy = documentProfile.chunkingStrategy();
        }
        return strategy != null && strategy.toLowerCase(Locale.ROOT).contains("table_row");
    }

    private void emitSheet(
            UUID libraryId,
            UUID documentId,
            UUID indexVersionId,
            ParsedDocument document,
            DocumentProfile documentProfile,
            LibraryProfile profile,
            ModelTokenizer tokenizer,
            List<DocumentChunk> chunks,
            String sheetLabel,
            List<StructuralBlock> rows,
            TableChunkConfig config,
            int sheetColumnCount
    ) {
        int maxTokens = config.chunkMaxTokens();
        int maxGroupRows = config.tableRowGroupMaxRows();
        int minIndexFields = config.tableIndexMinFields();
        boolean tokenWindow = config.usesTokenWindowGrouping();
        List<DocumentChunk> rowChunks = toRowChunks(
                libraryId,
                documentId,
                indexVersionId,
                document,
                documentProfile,
                profile,
                tokenizer,
                sheetLabel,
                rows,
                minIndexFields,
                sheetColumnCount
        );
        if (rowChunks.isEmpty()) {
            return;
        }
        String sheetContext = config.prependSheetContext() ? sheetContextLine(sheetLabel) : "";
        if (!tokenWindow && maxGroupRows <= 1) {
            for (int index = 0; index < rowChunks.size(); index++) {
                DocumentChunk rowChunk = rowChunks.get(index);
                String rowText = StructuredTableSummaryBuilder.stripPrefixes(rowChunk.content());
                String content = renderRowGroup(sheetContext, List.of(), rowText);
                addRowChunk(
                        chunks,
                        libraryId,
                        documentId,
                        indexVersionId,
                        document,
                        documentProfile,
                        profile,
                        tokenizer,
                        sheetLabel,
                        content,
                        rowChunk.metadata(),
                        index,
                        index,
                        config
                );
            }
            return;
        }

        List<String> currentRows = new ArrayList<>();
        int groupStart = -1;
        int groupEnd = -1;
        for (int index = 0; index < rowChunks.size(); index++) {
            DocumentChunk rowChunk = rowChunks.get(index);
            if (!Boolean.TRUE.equals(rowChunk.metadata().get("indexable"))) {
                if (!currentRows.isEmpty()) {
                    emitRowGroup(
                            chunks,
                            libraryId,
                            documentId,
                            indexVersionId,
                            document,
                            documentProfile,
                            profile,
                            tokenizer,
                            sheetLabel,
                            sheetContext,
                            currentRows,
                            groupStart,
                            groupEnd,
                            maxTokens,
                            minIndexFields,
                            sheetColumnCount,
                            config
                    );
                    currentRows.clear();
                    groupStart = -1;
                    groupEnd = -1;
                }
                addRowChunk(
                        chunks,
                        libraryId,
                        documentId,
                        indexVersionId,
                        document,
                        documentProfile,
                        profile,
                        tokenizer,
                        sheetLabel,
                        StructuredTableSummaryBuilder.stripPrefixes(rowChunk.content()),
                        rowChunk.metadata(),
                        index,
                        index,
                        config
                );
                continue;
            }
            String rowText = StructuredTableSummaryBuilder.stripPrefixes(rowChunk.content());
            String candidate = renderRowGroup(sheetContext, currentRows, rowText);
            boolean exceedsTokens = !currentRows.isEmpty() && tokenizer.count(candidate).tokens() > maxTokens;
            boolean exceedsRows = !currentRows.isEmpty()
                    && maxGroupRows < Integer.MAX_VALUE
                    && currentRows.size() >= maxGroupRows;
            if (exceedsTokens || exceedsRows) {
                emitRowGroup(
                        chunks,
                        libraryId,
                        documentId,
                        indexVersionId,
                        document,
                        documentProfile,
                        profile,
                        tokenizer,
                        sheetLabel,
                        sheetContext,
                        currentRows,
                        groupStart,
                        groupEnd,
                    maxTokens,
                    minIndexFields,
                    sheetColumnCount,
                    config
                );
                currentRows.clear();
                groupStart = -1;
            }
            if (groupStart < 0) {
                groupStart = index;
            }
            groupEnd = index;
            currentRows.add(rowText);
        }
        if (!currentRows.isEmpty()) {
            emitRowGroup(
                    chunks,
                    libraryId,
                    documentId,
                    indexVersionId,
                    document,
                    documentProfile,
                    profile,
                    tokenizer,
                    sheetLabel,
                    sheetContext,
                    currentRows,
                    groupStart,
                    groupEnd,
                    maxTokens,
                    minIndexFields,
                    sheetColumnCount,
                    config
            );
        }
    }

    private void emitRowGroup(
            List<DocumentChunk> chunks,
            UUID libraryId,
            UUID documentId,
            UUID indexVersionId,
            ParsedDocument document,
            DocumentProfile documentProfile,
            LibraryProfile profile,
            ModelTokenizer tokenizer,
            String sheetLabel,
            String sheetContext,
            List<String> rows,
            int rowStart,
            int rowEnd,
            int maxTokens,
            int minIndexFields,
            int sheetColumnCount,
            TableChunkConfig config
    ) {
        if (rows.size() == 1) {
            String single = renderRowGroup(sheetContext, List.of(), rows.getFirst());
            addRowChunk(
                    chunks,
                    libraryId,
                    documentId,
                    indexVersionId,
                    document,
                    documentProfile,
                    profile,
                    tokenizer,
                    sheetLabel,
                    single,
                    Map.of(
                            "indexable", TableRowIndexability.isIndexable(
                                    StructuredTableSummaryBuilder.stripPrefixes(single),
                                    minIndexFields,
                                    sheetColumnCount
                            )
                    ),
                    rowStart,
                    rowEnd,
                    config
            );
            return;
        }
        String text = renderRowGroup(sheetContext, rows, null);
        if (tokenizer.count(text).tokens() > maxTokens) {
            for (int index = 0; index < rows.size(); index++) {
                String single = renderRowGroup(sheetContext, List.of(), rows.get(index));
                addRowChunk(
                        chunks,
                        libraryId,
                        documentId,
                        indexVersionId,
                        document,
                        documentProfile,
                        profile,
                        tokenizer,
                        sheetLabel,
                        single,
                        Map.of(
                                "indexable", TableRowIndexability.isIndexable(
                                        rows.get(index),
                                        minIndexFields,
                                        sheetColumnCount
                                )
                        ),
                        rowStart + index,
                        rowStart + index,
                        config
                );
            }
            return;
        }
        addRowGroupChunk(
                chunks,
                libraryId,
                documentId,
                indexVersionId,
                document,
                documentProfile,
                profile,
                tokenizer,
                sheetLabel,
                text,
                rowStart,
                rowEnd,
                TableRowIndexability.isIndexable(
                        StructuredTableSummaryBuilder.stripPrefixes(text),
                        minIndexFields,
                        sheetColumnCount
                ),
                config
        );
    }

    private void addRowChunk(
            List<DocumentChunk> chunks,
            UUID libraryId,
            UUID documentId,
            UUID indexVersionId,
            ParsedDocument document,
            DocumentProfile documentProfile,
            LibraryProfile profile,
            ModelTokenizer tokenizer,
            String sheetLabel,
            String content,
            Map<String, Object> rowMetadata,
            int rowStart,
            int rowEnd,
            TableChunkConfig config
    ) {
        Map<String, Object> metadata = baseMetadata(document, documentProfile, sheetLabel);
        if (rowMetadata != null) {
            metadata.putAll(rowMetadata);
        }
        applyChunkStrategyMetadata(metadata, config, false);
        metadata.put("rowStart", rowStart);
        metadata.put("rowEnd", rowEnd);
        metadata.put("rowIndex", rowStart);
        if (rowStart != rowEnd) {
            metadata.put("rowRange", rowStart + ":" + rowEnd);
        } else {
            metadata.put("rowRange", String.valueOf(rowStart));
        }
        if (Boolean.FALSE.equals(metadata.get("indexable"))) {
            metadata.put("chunkOptimization", "layout-row-non-indexable");
        }
        chunks.add(new DocumentChunk(
                UUID.randomUUID(),
                documentId,
                libraryId,
                indexVersionId,
                content,
                tokenizer.count(content).tokens(),
                tokenizer.tokenizerId(),
                tokenizer.tokenizerVersion(),
                profile.embeddingModel(),
                "table_row",
                null,
                Map.copyOf(metadata)
        ));
    }

    private void addRowGroupChunk(
            List<DocumentChunk> chunks,
            UUID libraryId,
            UUID documentId,
            UUID indexVersionId,
            ParsedDocument document,
            DocumentProfile documentProfile,
            LibraryProfile profile,
            ModelTokenizer tokenizer,
            String sheetLabel,
            String content,
            int rowStart,
            int rowEnd,
            boolean indexable,
            TableChunkConfig config
    ) {
        Map<String, Object> metadata = baseMetadata(document, documentProfile, sheetLabel);
        applyChunkStrategyMetadata(metadata, config, true);
        metadata.put("indexable", indexable);
        metadata.put("rowStart", rowStart);
        metadata.put("rowEnd", rowEnd);
        if (rowStart != rowEnd) {
            metadata.put("mergedRowCount", rowEnd - rowStart + 1);
        }
        if (!indexable) {
            metadata.put("chunkOptimization", "layout-row-non-indexable");
        }
        chunks.add(new DocumentChunk(
                UUID.randomUUID(),
                documentId,
                libraryId,
                indexVersionId,
                content,
                tokenizer.count(content).tokens(),
                tokenizer.tokenizerId(),
                tokenizer.tokenizerVersion(),
                profile.embeddingModel(),
                "table_row_group",
                null,
                Map.copyOf(metadata)
        ));
    }

    private static String sheetContextLine(String sheetLabel) {
        String label = sheetLabel == null || sheetLabel.isBlank() ? "Table" : sheetLabel.trim();
        return "[Sheet: " + label + "]";
    }

    private static String renderRowGroup(String sheetContext, List<String> rows, String extraRow) {
        StringBuilder builder = new StringBuilder(sheetContext);
        for (String row : rows) {
            if (row == null || row.isBlank()) {
                continue;
            }
            builder.append('\n').append(row.trim());
        }
        if (extraRow != null && !extraRow.isBlank()) {
            builder.append('\n').append(extraRow.trim());
        }
        return builder.toString().trim();
    }

    private static List<DocumentChunk> toRowChunks(
            UUID libraryId,
            UUID documentId,
            UUID indexVersionId,
            ParsedDocument document,
            DocumentProfile documentProfile,
            LibraryProfile profile,
            ModelTokenizer tokenizer,
            String sheetLabel,
            List<StructuralBlock> rows,
            int minIndexFields,
            int sheetColumnCount
    ) {
        List<DocumentChunk> rowChunks = new ArrayList<>(rows.size());
        for (StructuralBlock block : rows) {
            String content = block.content() == null ? "" : block.content().trim();
            if (content.isBlank()) {
                continue;
            }
            Map<String, Object> metadata = baseMetadata(document, documentProfile, sheetLabel);
            metadata.putAll(block.metadata());
            metadata.put("chunkRole", "flat");
            boolean indexable = resolveIndexable(block, content, minIndexFields, sheetColumnCount);
            metadata.put("indexable", indexable);
            if (!indexable) {
                metadata.put("populatedFieldCount", TableRowIndexability.countPopulatedFields(content));
            }
            rowChunks.add(new DocumentChunk(
                    UUID.randomUUID(),
                    documentId,
                    libraryId,
                    indexVersionId,
                    content,
                    tokenizer.count(content).tokens(),
                    tokenizer.tokenizerId(),
                    tokenizer.tokenizerVersion(),
                    profile.embeddingModel(),
                    "table_row",
                    null,
                    Map.copyOf(metadata)
            ));
        }
        return rowChunks;
    }

    private static boolean resolveIndexable(
            StructuralBlock block,
            String content,
            int minIndexFields,
            int sheetColumnCount
    ) {
        if (block.metadata() != null) {
            Object hint = block.metadata().get("indexableHint");
            if (hint instanceof Boolean value) {
                return value;
            }
        }
        return TableRowIndexability.isIndexable(content, minIndexFields, sheetColumnCount);
    }

    private static int sheetColumnCount(List<StructuralBlock> rows) {
        int max = 0;
        for (StructuralBlock block : rows) {
            if (block.metadata() == null) {
                continue;
            }
            Object keys = block.metadata().get("columnKeys");
            if (keys instanceof List<?> list) {
                max = Math.max(max, list.size());
            }
            Object columnEnd = block.metadata().get("columnEnd");
            if (columnEnd instanceof Number number) {
                max = Math.max(max, number.intValue() + 1);
            }
        }
        return max;
    }

    private static Map<String, List<StructuralBlock>> groupRowsBySheet(ParsedDocument document) {
        Map<String, List<StructuralBlock>> bySheet = new LinkedHashMap<>();
        for (StructuralBlock block : document.blocks()) {
            if (!"table_row".equals(block.blockType())) {
                continue;
            }
            String sheet = String.valueOf(block.metadata().getOrDefault("sheetName", "Table"));
            bySheet.computeIfAbsent(sheet, ignored -> new ArrayList<>()).add(block);
        }
        return bySheet;
    }

    private static Map<String, Object> baseMetadata(
            ParsedDocument document,
            DocumentProfile documentProfile,
            String sheetLabel
    ) {
        Map<String, Object> metadata = new HashMap<>();
        if (document.metadata() != null) {
            metadata.putAll(document.metadata());
        }
        metadata.put("sourceUri", document.sourceUri());
        metadata.put("title", document.title());
        metadata.put("contentFamily", document.contentFamily().name());
        metadata.put("sheetName", sheetLabel);
        metadata.put("tableContext", sheetLabel);
        if (documentProfile != null) {
            metadata.put("documentProfileCode", documentProfile.code());
            metadata.put("parserCode", documentProfile.parserCode());
            metadata.put("chunkingStrategy", documentProfile.chunkingStrategy());
        }
        return metadata;
    }

    private static void applyChunkStrategyMetadata(
            Map<String, Object> metadata,
            TableChunkConfig config,
            boolean grouped
    ) {
        metadata.putIfAbsent("indexable", true);
        metadata.put("chunkTemplate", "flat");
        metadata.put("chunkEngine", "smart-table");
        metadata.put("chunkingStrategy", config.chunkingStrategy());
        metadata.put("chunkMaxTokens", config.chunkMaxTokens());
        if (grouped) {
            metadata.put("chunkRole", "table_row_group");
            metadata.put("strategy", "table-row-group");
            metadata.put("rowGroupingMode", config.rowGroupingMode().name());
        } else {
            metadata.put("chunkRole", "table_row");
            metadata.put("strategy", config.usesTokenWindowGrouping() ? "table-row-token-window" : "table-row");
            metadata.put("rowGroupingMode", config.rowGroupingMode().name());
        }
    }
}
