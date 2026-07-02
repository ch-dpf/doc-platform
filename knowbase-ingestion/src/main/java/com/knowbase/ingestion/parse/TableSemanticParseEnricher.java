package com.knowbase.ingestion.parse;

import com.knowbase.ingestion.StructuralBlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Normalizes table-row semantic metadata for citation and TableGrid consumers.
 */
public final class TableSemanticParseEnricher {

    private TableSemanticParseEnricher() {
    }

    public static List<StructuralBlock> enrich(List<StructuralBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return blocks;
        }
        List<StructuralBlock> enriched = new ArrayList<>(blocks.size());
        for (StructuralBlock block : blocks) {
            if (!"table_row".equals(block.blockType())) {
                enriched.add(block);
                continue;
            }
            Map<String, Object> metadata = new HashMap<>(block.metadata());
            stampSpanSummary(metadata);
            metadata.putIfAbsent("tableSemanticVersion", "v1");
            enriched.add(new StructuralBlock(
                    block.blockType(),
                    block.level(),
                    block.content(),
                    block.ordinal(),
                    Map.copyOf(metadata)
            ));
        }
        return List.copyOf(enriched);
    }

    @SuppressWarnings("unchecked")
    private static void stampSpanSummary(Map<String, Object> metadata) {
        Object raw = metadata.get("cellCoordinates");
        if (!(raw instanceof List<?> cells) || cells.isEmpty()) {
            return;
        }
        int maxColumnSpan = 1;
        int maxRowSpan = 1;
        boolean anyMerged = false;
        for (Object item : cells) {
            if (!(item instanceof Map<?, ?> cell)) {
                continue;
            }
            int columnSpan = intValue(cell.get("columnSpan"), 1);
            int rowSpan = intValue(cell.get("rowSpan"), 1);
            maxColumnSpan = Math.max(maxColumnSpan, columnSpan);
            maxRowSpan = Math.max(maxRowSpan, rowSpan);
            if (Boolean.TRUE.equals(cell.get("merged"))) {
                anyMerged = true;
            }
        }
        if (anyMerged) {
            metadata.put("hasMergedCells", true);
        }
        metadata.put("maxColumnSpan", maxColumnSpan);
        metadata.put("maxRowSpan", maxRowSpan);
    }

    private static int intValue(Object value, int defaultValue) {
        if (value instanceof Number number) {
            int parsed = number.intValue();
            return parsed <= 0 ? defaultValue : parsed;
        }
        return defaultValue;
    }
}
