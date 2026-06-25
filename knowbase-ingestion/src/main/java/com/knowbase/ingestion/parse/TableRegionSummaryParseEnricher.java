package com.knowbase.ingestion.parse;

import com.knowbase.ingestion.StructuralBlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Injects parse-stage {@code table_summary} blocks for each table region (plan §3.3).
 */
public final class TableRegionSummaryParseEnricher {

    private TableRegionSummaryParseEnricher() {
    }

    public static List<StructuralBlock> enrich(List<StructuralBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return List.of();
        }
        Map<String, List<StructuralBlock>> regions = new HashMap<>();
        for (StructuralBlock block : blocks) {
            if (!"table_row".equals(block.blockType()) || block.metadata() == null) {
                continue;
            }
            Object regionId = block.metadata().get("tableRegionId");
            if (regionId == null) {
                continue;
            }
            regions.computeIfAbsent(String.valueOf(regionId), ignored -> new ArrayList<>()).add(block);
        }
        if (regions.isEmpty()) {
            return List.copyOf(blocks);
        }
        Map<String, Boolean> summaryInserted = new HashMap<>();
        List<StructuralBlock> enriched = new ArrayList<>();
        int ordinal = 0;
        for (StructuralBlock block : blocks) {
            if ("table_row".equals(block.blockType()) && block.metadata() != null) {
                Object regionId = block.metadata().get("tableRegionId");
                if (regionId != null) {
                    String key = String.valueOf(regionId);
                    if (!Boolean.TRUE.equals(summaryInserted.get(key))) {
                        enriched.add(summaryBlock(regions.get(key), ordinal++));
                        summaryInserted.put(key, true);
                    }
                }
            }
            enriched.add(copyWithOrdinal(block, ordinal++));
        }
        return List.copyOf(enriched);
    }

    private static StructuralBlock summaryBlock(List<StructuralBlock> regionBlocks, int ordinal) {
        StructuralBlock first = regionBlocks.getFirst();
        Map<String, Object> base = first.metadata();
        String label = base.get("tableRegionLabel") == null
                ? "table-" + base.get("tableRegionId")
                : String.valueOf(base.get("tableRegionLabel"));
        long dataRows = regionBlocks.stream()
                .filter(block -> "DATA".equals(String.valueOf(block.metadata().get("rowRole"))))
                .count();
        List<Integer> pages = regionBlocks.stream()
                .map(block -> block.metadata().get("pageNumber"))
                .filter(Number.class::isInstance)
                .map(value -> ((Number) value).intValue())
                .distinct()
                .sorted()
                .toList();
        boolean continuation = pages.size() > 1
                || regionBlocks.stream().anyMatch(block -> Boolean.TRUE.equals(block.metadata().get("tableContinuation")));
        String sheet = base.get("sheetName") == null ? "" : "Sheet: " + base.get("sheetName") + " · ";
        String content = "[表区摘要] " + sheet + label + " · 数据行: " + dataRows + " · 总行: " + regionBlocks.size();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("boundaryType", "table_summary");
        metadata.put("rowRole", "TABLE_SUMMARY");
        metadata.put("indexableHint", false);
        metadata.put("tableRegionId", base.get("tableRegionId"));
        metadata.put("tableRegionLabel", label);
        if (base.get("sheetName") != null) {
            metadata.put("sheetName", base.get("sheetName"));
        }
        if (base.get("pageNumber") != null) {
            metadata.put("pageNumber", base.get("pageNumber"));
        }
        if (base.get("tableFormat") != null) {
            metadata.put("tableFormat", base.get("tableFormat"));
        }
        metadata.put("tableRegionRowCount", regionBlocks.size());
        metadata.put("tableRegionDataRowCount", dataRows);
        if (continuation) {
            metadata.put("tableContinuation", true);
        }
        if (!pages.isEmpty()) {
            metadata.put("tableRegionPages", pages);
            metadata.put("pageNumber", pages.getFirst());
        }
        return new StructuralBlock("table_summary", 0, content, ordinal, Map.copyOf(metadata));
    }

    private static StructuralBlock copyWithOrdinal(StructuralBlock block, int ordinal) {
        return new StructuralBlock(block.blockType(), block.level(), block.content(), ordinal, block.metadata());
    }
}
