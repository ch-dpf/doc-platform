package com.knowbase.ingestion.parse;

import com.knowbase.ingestion.StructuralBlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Assigns {@code tableRegionId} to consecutive {@code table_row} blocks that lack region metadata
 * (e.g. OCR / VLM pipe rows without explicit regions).
 */
public final class TableRegionIdParseEnricher {

    private TableRegionIdParseEnricher() {
    }

    public static List<StructuralBlock> enrich(List<StructuralBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return blocks;
        }
        int nextRegionId = maxExistingRegionId(blocks) + 1;
        List<StructuralBlock> enriched = new ArrayList<>(blocks.size());
        int index = 0;
        while (index < blocks.size()) {
            StructuralBlock block = blocks.get(index);
            if (needsRegion(block)) {
                int runStart = index;
                while (index < blocks.size() && needsRegion(blocks.get(index))) {
                    index++;
                }
                int regionId = nextRegionId++;
                String label = inferRegionLabel(blocks.get(runStart), regionId);
                for (int row = runStart; row < index; row++) {
                    enriched.add(assignRegion(blocks.get(row), regionId, label, row - runStart));
                }
            } else {
                enriched.add(block);
                index++;
            }
        }
        return List.copyOf(enriched);
    }

    private static boolean needsRegion(StructuralBlock block) {
        if (block == null || !"table_row".equals(block.blockType())) {
            return false;
        }
        Map<String, Object> metadata = block.metadata();
        return metadata == null || metadata.get("tableRegionId") == null;
    }

    private static int maxExistingRegionId(List<StructuralBlock> blocks) {
        int max = -1;
        for (StructuralBlock block : blocks) {
            if (block.metadata() == null) {
                continue;
            }
            Object regionId = block.metadata().get("tableRegionId");
            if (regionId instanceof Number number) {
                max = Math.max(max, number.intValue());
            }
        }
        return max;
    }

    private static String inferRegionLabel(StructuralBlock firstRow, int regionId) {
        if (firstRow.metadata() != null && firstRow.metadata().get("tableRegionLabel") != null) {
            return String.valueOf(firstRow.metadata().get("tableRegionLabel"));
        }
        String format = firstRow.metadata() == null ? null : String.valueOf(firstRow.metadata().get("tableFormat"));
        if ("markdown".equals(format)) {
            return "md-table-" + regionId;
        }
        if ("pdf".equals(format)) {
            return "pdf-table-" + regionId;
        }
        Object page = firstRow.metadata() == null ? null : firstRow.metadata().get("pageNumber");
        if (page != null) {
            return "table-p" + page + "-" + regionId;
        }
        return "table-" + regionId;
    }

    private static StructuralBlock assignRegion(StructuralBlock block, int regionId, String label, int rowOffset) {
        Map<String, Object> metadata = new HashMap<>(block.metadata() == null ? Map.of() : block.metadata());
        metadata.put("tableRegionId", regionId);
        metadata.putIfAbsent("tableRegionLabel", label);
        metadata.putIfAbsent("boundaryType", "table_row");
        metadata.putIfAbsent("layoutRole", "table");
        if (!metadata.containsKey("rowRole")) {
            metadata.put("rowRole", rowOffset == 0 ? "HEADER" : "DATA");
        }
        metadata.putIfAbsent("indexableHint", !"HEADER".equals(String.valueOf(metadata.get("rowRole"))));
        return new StructuralBlock(
                block.blockType(),
                block.level(),
                block.content(),
                block.ordinal(),
                Map.copyOf(metadata)
        );
    }
}
