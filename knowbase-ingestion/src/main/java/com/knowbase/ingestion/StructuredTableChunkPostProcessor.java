package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentChunk;
import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.domain.status.ContentFamily;
import com.knowbase.tokenizer.ModelTokenizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Product-oriented chunk optimization for Excel/CSV report libraries:
 * sheet context in embeddable text, document-level rule summary, adjacent small-row merging, dedupe.
 */
public final class StructuredTableChunkPostProcessor implements ChunkPostProcessor {

    private static final String SHEET_CONTEXT_PREFIX = "[Sheet: %s]\n";

    @Override
    public boolean supports(ChunkPostProcessContext context) {
        if (context == null || context.documentProfile() == null) {
            return false;
        }
        if (!readBoolean(context.documentProfile(), "tableChunkPostProcess", true)) {
            return false;
        }
        if (context.document().contentFamily() == ContentFamily.STRUCTURED_TABLE) {
            return true;
        }
        String strategy = context.documentProfile().chunkingStrategy();
        return strategy != null && strategy.toLowerCase(Locale.ROOT).contains("table_row");
    }

    @Override
    public List<DocumentChunk> process(List<DocumentChunk> chunks, ChunkPostProcessContext context) {
        Objects.requireNonNull(chunks, "chunks");
        Objects.requireNonNull(context, "context");
        if (!supports(context)) {
            return chunks;
        }

        DocumentProfile documentProfile = context.documentProfile();
        LibraryProfile libraryProfile = context.libraryProfile();
        ModelTokenizer tokenizer = context.tokenizer();
        int maxTokens = libraryProfile == null ? 384 : libraryProfile.chunkMaxTokens();
        int mergeBelowTokens = readInt(documentProfile, "tableRowMergeBelowTokens", 64);
        boolean prependSheetContext = readBoolean(documentProfile, "prependSheetContext", true);
        boolean emitDocumentSummary = readBoolean(documentProfile, "emitDocumentSummary", false);
        boolean mergeSmallRowChunks = readBoolean(documentProfile, "mergeSmallRowChunks", true);
        boolean deduplicateChunks = readBoolean(documentProfile, "deduplicateChunks", true);
        boolean smartTableChunked = usesSmartTableChunks(chunks);

        List<DocumentChunk> nonIndexable = new ArrayList<>();
        List<DocumentChunk> indexable = new ArrayList<>();
        for (DocumentChunk chunk : chunks) {
            if (isIndexable(chunk)) {
                indexable.add(chunk);
            } else {
                nonIndexable.add(chunk);
            }
        }

        List<DocumentChunk> optimized = new ArrayList<>(indexable);
        if (prependSheetContext && !smartTableChunked) {
            optimized = prependTableContext(optimized, context.document(), tokenizer);
        }
        if (mergeSmallRowChunks && !smartTableChunked) {
            optimized = mergeAdjacentSmallRows(optimized, maxTokens, mergeBelowTokens, tokenizer);
        }
        if (deduplicateChunks) {
            optimized = deduplicate(optimized);
        }
        if (emitDocumentSummary) {
            optimized = maybeAddDocumentSummary(optimized, context, tokenizer);
        }
        optimized = reassignFlatOrdinals(optimized);

        List<DocumentChunk> result = new ArrayList<>(nonIndexable.size() + optimized.size());
        result.addAll(nonIndexable);
        result.addAll(optimized);
        return result;
    }

    private List<DocumentChunk> prependTableContext(
            List<DocumentChunk> chunks,
            ParsedDocument document,
            ModelTokenizer tokenizer
    ) {
        List<DocumentChunk> enriched = new ArrayList<>(chunks.size());
        for (DocumentChunk chunk : chunks) {
            if (!isTableDataChunk(chunk)) {
                enriched.add(chunk);
                continue;
            }
            String label = tableContextLabel(chunk, document);
            if (label == null || label.isBlank()) {
                enriched.add(chunk);
                continue;
            }
            String prefix = sheetContextPrefix(label);
            if (chunk.content() != null && chunk.content().startsWith(prefix.trim())) {
                enriched.add(chunk);
                continue;
            }
            String content = prefix + chunk.content();
            enriched.add(copyChunk(chunk, content, tokenizer, Map.of(
                    "tableContext", label,
                    "chunkOptimization", "prepend-sheet-context"
            )));
        }
        return enriched;
    }

    private List<DocumentChunk> mergeAdjacentSmallRows(
            List<DocumentChunk> chunks,
            int maxTokens,
            int mergeBelowTokens,
            ModelTokenizer tokenizer
    ) {
        List<DocumentChunk> ordered = sortByOrdinal(chunks);
        List<DocumentChunk> merged = new ArrayList<>();
        List<DocumentChunk> group = new ArrayList<>();

        for (DocumentChunk chunk : ordered) {
            if (!isMergeCandidate(chunk)) {
                flushRowGroup(group, merged, maxTokens, mergeBelowTokens, tokenizer);
                group.clear();
                merged.add(chunk);
                continue;
            }
            if (group.isEmpty()) {
                group.add(chunk);
                continue;
            }
            if (!sameTableContext(group.getLast(), chunk)) {
                flushRowGroup(group, merged, maxTokens, mergeBelowTokens, tokenizer);
                group.clear();
                group.add(chunk);
                continue;
            }
            int combinedTokens = groupTokens(group) + chunk.tokenCount() + 2;
            boolean shouldMerge = combinedTokens <= maxTokens
                    && (chunk.tokenCount() < mergeBelowTokens || groupHasSmallRow(group, mergeBelowTokens));
            if (shouldMerge) {
                group.add(chunk);
            } else {
                flushRowGroup(group, merged, maxTokens, mergeBelowTokens, tokenizer);
                group.clear();
                group.add(chunk);
            }
        }
        flushRowGroup(group, merged, maxTokens, mergeBelowTokens, tokenizer);
        return merged;
    }

    private void flushRowGroup(
            List<DocumentChunk> group,
            List<DocumentChunk> output,
            int maxTokens,
            int mergeBelowTokens,
            ModelTokenizer tokenizer
    ) {
        if (group.isEmpty()) {
            return;
        }
        if (group.size() == 1) {
            output.add(group.getFirst());
            return;
        }
        if (groupTokens(group) > maxTokens) {
            for (DocumentChunk chunk : group) {
                output.add(chunk);
            }
            return;
        }
        output.add(mergeRowGroup(group, tokenizer));
    }

    private DocumentChunk mergeRowGroup(List<DocumentChunk> group, ModelTokenizer tokenizer) {
        DocumentChunk first = group.getFirst();
        DocumentChunk last = group.getLast();
        StringBuilder content = new StringBuilder();
        for (int index = 0; index < group.size(); index++) {
            if (index > 0) {
                content.append("\n\n");
            }
            content.append(stripTableContextPrefix(group.get(index).content()));
        }
        Map<String, Object> metadata = new HashMap<>(first.metadata());
        metadata.put("chunkRole", "table_row_group");
        metadata.put("chunkOptimization", "merge-small-rows");
        metadata.put("mergedRowCount", group.size());
        metadata.put("rowStart", metadataValue(first.metadata(), "rowStart", metadataValue(first.metadata(), "rowIndex", null)));
        metadata.put("rowEnd", metadataValue(last.metadata(), "rowEnd", metadataValue(last.metadata(), "rowIndex", null)));
        if (metadata.get("rowStart") != null && metadata.get("rowEnd") != null) {
            metadata.put("rowRange", metadata.get("rowStart") + ":" + metadata.get("rowEnd"));
        }
        String mergedContent = reapplyTableContextPrefix(first.content(), content.toString());
        return copyChunk(first, mergedContent, tokenizer, metadata);
    }

    private List<DocumentChunk> deduplicate(List<DocumentChunk> chunks) {
        List<DocumentChunk> deduped = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (DocumentChunk chunk : chunks) {
            String key = normalizeContentKey(chunk.content());
            if (key.isBlank() || seen.add(key)) {
                deduped.add(chunk);
            }
        }
        return deduped;
    }

    private List<DocumentChunk> maybeAddDocumentSummary(
            List<DocumentChunk> chunks,
            ChunkPostProcessContext context,
            ModelTokenizer tokenizer
    ) {
        if (hasDocumentSummary(chunks)) {
            return chunks;
        }
        Map<String, List<DocumentChunk>> bySheet = groupRowsBySheet(chunks, context.document());
        if (bySheet.isEmpty()) {
            return chunks;
        }
        DocumentChunk template = chunks.stream()
                .filter(StructuredTableChunkPostProcessor::isTableDataChunk)
                .findFirst()
                .orElse(chunks.getFirst());
        DocumentChunk summary = buildDocumentSummaryChunk(template, bySheet, context, tokenizer);
        List<DocumentChunk> combined = new ArrayList<>(chunks.size() + 1);
        combined.add(summary);
        combined.addAll(chunks);
        return combined;
    }

    private static Map<String, List<DocumentChunk>> groupRowsBySheet(List<DocumentChunk> chunks, ParsedDocument document) {
        Map<String, List<DocumentChunk>> bySheet = new LinkedHashMap<>();
        for (DocumentChunk chunk : chunks) {
            if (!isTableDataChunkStatic(chunk)) {
                continue;
            }
            String label = tableContextLabelStatic(chunk, document);
            bySheet.computeIfAbsent(label == null ? "" : label, ignored -> new ArrayList<>()).add(chunk);
        }
        return bySheet;
    }

    private DocumentChunk buildDocumentSummaryChunk(
            DocumentChunk template,
            Map<String, List<DocumentChunk>> rowsBySheet,
            ChunkPostProcessContext context,
            ModelTokenizer tokenizer
    ) {
        String content = StructuredTableSummaryBuilder.buildDocumentSummaryText(context.document(), rowsBySheet);
        Map<String, Object> metadata = summaryMetadata(template, context, "document_summary");
        metadata.put("chunkOptimization", "document-summary");
        metadata.put("summarySheetCount", rowsBySheet.size());
        return new DocumentChunk(
                UUID.randomUUID(),
                template.documentId(),
                template.libraryId(),
                template.indexVersionId(),
                content,
                tokenizer.count(content).tokens(),
                tokenizer.tokenizerId(),
                tokenizer.tokenizerVersion(),
                template.embeddingModel(),
                "document_summary",
                null,
                Map.copyOf(metadata)
        );
    }

    private static Map<String, Object> summaryMetadata(
            DocumentChunk template,
            ChunkPostProcessContext context,
            String chunkRole
    ) {
        Map<String, Object> metadata = new HashMap<>();
        if (template.metadata() != null) {
            metadata.putAll(template.metadata());
        }
        metadata.put("chunkRole", chunkRole);
        metadata.put("chunkTemplate", "parent-child");
        metadata.put("strategy", "table-summary");
        metadata.put("indexable", true);
        metadata.put("contentFamily", ContentFamily.STRUCTURED_TABLE.name());
        if (context.documentProfile() != null) {
            metadata.put("documentProfileCode", context.documentProfile().code());
            metadata.put("parserCode", context.documentProfile().parserCode());
            metadata.put("chunkingStrategy", context.documentProfile().chunkingStrategy());
        }
        return metadata;
    }

    private static boolean usesSmartTableChunks(List<DocumentChunk> chunks) {
        return chunks.stream().anyMatch(chunk -> {
            if (chunk.metadata() == null) {
                return false;
            }
            return "smart-table".equals(String.valueOf(chunk.metadata().get("chunkEngine")));
        });
    }

    private static boolean hasDocumentSummary(List<DocumentChunk> chunks) {
        for (DocumentChunk chunk : chunks) {
            if ("document_summary".equals(chunk.chunkBoundaryType())) {
                return true;
            }
            if ("document_summary".equals(metadataTextStatic(chunk, "chunkRole"))) {
                return true;
            }
        }
        return false;
    }

    private boolean isSummaryChunk(DocumentChunk chunk) {
        return isSummaryChunkStatic(chunk);
    }

    private static List<DocumentChunk> reassignFlatOrdinals(List<DocumentChunk> chunks) {
        List<DocumentChunk> reassigned = new ArrayList<>(chunks.size());
        int ordinal = 0;
        for (DocumentChunk chunk : chunks) {
            Map<String, Object> metadata = new HashMap<>(chunk.metadata() == null ? Map.of() : chunk.metadata());
            metadata.put("flatOrdinal", ordinal++);
            reassigned.add(new DocumentChunk(
                    chunk.chunkId(),
                    chunk.documentId(),
                    chunk.libraryId(),
                    chunk.indexVersionId(),
                    chunk.content(),
                    chunk.tokenCount(),
                    chunk.tokenizerId(),
                    chunk.tokenizerVersion(),
                    chunk.embeddingModel(),
                    chunk.chunkBoundaryType(),
                    chunk.parentChunkId(),
                    Map.copyOf(metadata)
            ));
        }
        return reassigned;
    }

    private static DocumentChunk copyChunk(
            DocumentChunk source,
            String content,
            ModelTokenizer tokenizer,
            Map<String, Object> extraMetadata
    ) {
        Map<String, Object> metadata = new HashMap<>(source.metadata() == null ? Map.of() : source.metadata());
        metadata.putAll(extraMetadata);
        return new DocumentChunk(
                source.chunkId(),
                source.documentId(),
                source.libraryId(),
                source.indexVersionId(),
                content,
                tokenizer.count(content).tokens(),
                source.tokenizerId(),
                source.tokenizerVersion(),
                source.embeddingModel(),
                source.chunkBoundaryType(),
                source.parentChunkId(),
                Map.copyOf(metadata)
        );
    }

    private static List<DocumentChunk> sortByOrdinal(List<DocumentChunk> chunks) {
        List<DocumentChunk> sorted = new ArrayList<>(chunks);
        sorted.sort((left, right) -> Integer.compare(ordinal(left), ordinal(right)));
        return sorted;
    }

    private static int ordinal(DocumentChunk chunk) {
        Object flat = chunk.metadata() == null ? null : chunk.metadata().get("flatOrdinal");
        if (flat instanceof Number number) {
            return number.intValue();
        }
        Object structure = chunk.metadata() == null ? null : chunk.metadata().get("structureOrdinal");
        if (structure instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    private static int groupTokens(List<DocumentChunk> group) {
        int total = 0;
        for (DocumentChunk chunk : group) {
            total += chunk.tokenCount();
        }
        return total + Math.max(0, group.size() - 1) * 2;
    }

    private static boolean groupHasSmallRow(List<DocumentChunk> group, int mergeBelowTokens) {
        return group.stream().anyMatch(chunk -> chunk.tokenCount() < mergeBelowTokens);
    }

    private static boolean sameTableContext(DocumentChunk left, DocumentChunk right) {
        return Objects.equals(tableContextLabel(left, null), tableContextLabel(right, null));
    }

    private static String tableContextLabel(DocumentChunk chunk, ParsedDocument document) {
        return tableContextLabelStatic(chunk, document);
    }

    private static String tableContextLabelStatic(DocumentChunk chunk, ParsedDocument document) {
        if (chunk.metadata() != null) {
            Object sheetName = chunk.metadata().get("sheetName");
            if (sheetName != null && !String.valueOf(sheetName).isBlank()) {
                return String.valueOf(sheetName).trim();
            }
            Object tableContext = chunk.metadata().get("tableContext");
            if (tableContext != null && !String.valueOf(tableContext).isBlank()) {
                return String.valueOf(tableContext).trim();
            }
        }
        if (document != null && document.metadata() != null) {
            if ("csv".equalsIgnoreCase(String.valueOf(document.metadata().get("tableFormat")))) {
                return firstNonBlank(document.title(), "CSV");
            }
        }
        if (document != null) {
            return firstNonBlank(document.title(), "Table");
        }
        return "Table";
    }

    private static String sheetContextPrefix(String label) {
        return String.format(SHEET_CONTEXT_PREFIX, label);
    }

    private static String stripTableContextPrefix(String content) {
        if (content == null) {
            return "";
        }
        int newline = content.indexOf('\n');
        if (newline > 0 && content.startsWith("[Sheet: ") && content.contains("]\n")) {
            return content.substring(newline + 1);
        }
        if (newline > 0 && content.startsWith("[Table: ") && content.contains("]\n")) {
            return content.substring(newline + 1);
        }
        return content;
    }

    private static String reapplyTableContextPrefix(String originalContent, String body) {
        if (originalContent == null) {
            return body;
        }
        int newline = originalContent.indexOf('\n');
        if (newline > 0 && (originalContent.startsWith("[Sheet: ") || originalContent.startsWith("[Table: "))) {
            return originalContent.substring(0, newline + 1) + body;
        }
        return body;
    }

    @SuppressWarnings("unchecked")
    private static List<String> extractHeaders(DocumentChunk chunk) {
        if (chunk.metadata() == null) {
            return List.of();
        }
        Object headerPath = chunk.metadata().get("headerPath");
        if (headerPath instanceof List<?> list) {
            List<String> headers = new ArrayList<>();
            for (Object item : list) {
                if (item != null && !String.valueOf(item).isBlank()) {
                    headers.add(String.valueOf(item).trim());
                }
            }
            if (!headers.isEmpty()) {
                return headers;
            }
        }
        return List.of();
    }

    private static boolean isIndexable(DocumentChunk chunk) {
        if (chunk.parentChunkId() != null) {
            return true;
        }
        if (chunk.metadata() == null) {
            return true;
        }
        Object indexable = chunk.metadata().get("indexable");
        if (indexable instanceof Boolean value) {
            return value;
        }
        return true;
    }

    private static boolean isTableDataChunk(DocumentChunk chunk) {
        return isTableDataChunkStatic(chunk);
    }

    private static boolean isTableDataChunkStatic(DocumentChunk chunk) {
        if (isSummaryChunkStatic(chunk)) {
            return false;
        }
        String role = metadataTextStatic(chunk, "chunkRole");
        return "flat".equals(role) || "child".equals(role) || "table_row".equals(role)
                || "table_row_group".equals(role) || "table_row".equals(chunk.chunkBoundaryType());
    }

    private static boolean isSummaryChunkStatic(DocumentChunk chunk) {
        if ("document_summary".equals(chunk.chunkBoundaryType())) {
            return true;
        }
        return "document_summary".equals(metadataTextStatic(chunk, "chunkRole"));
    }

    private static boolean isMergeCandidate(DocumentChunk chunk) {
        if (!isTableDataChunk(chunk)) {
            return false;
        }
        String role = metadataText(chunk, "chunkRole");
        return "flat".equals(role) || "child".equals(role) || "table_row".equals(role)
                || "table_row".equals(chunk.chunkBoundaryType());
    }

    private static String metadataText(DocumentChunk chunk, String key) {
        return metadataTextStatic(chunk, key);
    }

    private static String metadataTextStatic(DocumentChunk chunk, String key) {
        if (chunk.metadata() == null) {
            return "";
        }
        Object value = chunk.metadata().get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static Object metadataValue(Map<String, Object> metadata, String key, Object fallback) {
        if (metadata == null || !metadata.containsKey(key)) {
            return fallback;
        }
        return metadata.get(key);
    }

    private static String normalizeContentKey(String content) {
        if (content == null) {
            return "";
        }
        return content.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static boolean readBoolean(DocumentProfile profile, String key, boolean defaultValue) {
        Object value = option(profile, key);
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value != null) {
            return Boolean.parseBoolean(String.valueOf(value).trim());
        }
        return defaultValue;
    }

    private static int readInt(DocumentProfile profile, String key, int defaultValue) {
        Object value = option(profile, key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value).trim());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private static Object option(DocumentProfile profile, String key) {
        if (profile == null || profile.options() == null) {
            return null;
        }
        return profile.options().get(key);
    }
}
