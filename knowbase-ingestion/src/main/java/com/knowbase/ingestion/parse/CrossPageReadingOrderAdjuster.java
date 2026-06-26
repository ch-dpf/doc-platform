package com.knowbase.ingestion.parse;

import com.knowbase.ingestion.StructuralBlock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Keeps multi-page table regions contiguous in reading order.
 */
public final class CrossPageReadingOrderAdjuster {

    private CrossPageReadingOrderAdjuster() {
    }

    public static List<StructuralBlock> adjust(List<StructuralBlock> blocks) {
        if (blocks == null || blocks.size() <= 1) {
            return blocks;
        }
        List<StructuralBlock> working = new ArrayList<>(blocks);
        Map<Integer, List<StructuralBlock>> tableGroups = groupTableRows(working);
        for (Map.Entry<Integer, List<StructuralBlock>> entry : tableGroups.entrySet()) {
            List<StructuralBlock> group = entry.getValue();
            if (!spansPages(group)) {
                continue;
            }
            List<StructuralBlock> sorted = new ArrayList<>(group);
            sorted.sort(tableRowComparator());
            int anchor = anchorIndex(working, entry.getKey());
            working.removeIf(block -> belongsToRegion(block, entry.getKey()));
            working.addAll(Math.min(anchor, working.size()), sorted);
        }
        return renumber(working);
    }

    private static Map<Integer, List<StructuralBlock>> groupTableRows(List<StructuralBlock> blocks) {
        Map<Integer, List<StructuralBlock>> grouped = new LinkedHashMap<>();
        for (StructuralBlock block : blocks) {
            Integer regionId = tableRegionId(block);
            if (regionId == null || !"table_row".equals(block.blockType())) {
                continue;
            }
            grouped.computeIfAbsent(regionId, ignored -> new ArrayList<>()).add(block);
        }
        return grouped;
    }

    private static boolean spansPages(List<StructuralBlock> group) {
        Set<Integer> pages = new HashSet<>();
        for (StructuralBlock block : group) {
            pages.add(pageNumber(block));
        }
        return pages.size() > 1;
    }

    private static int anchorIndex(List<StructuralBlock> blocks, int regionId) {
        for (int index = 0; index < blocks.size(); index++) {
            if (belongsToRegion(blocks.get(index), regionId)) {
                return index;
            }
        }
        return blocks.size();
    }

    private static boolean belongsToRegion(StructuralBlock block, int regionId) {
        Integer id = tableRegionId(block);
        return id != null && id == regionId && "table_row".equals(block.blockType());
    }

    private static Comparator<StructuralBlock> tableRowComparator() {
        return Comparator
                .comparingInt(CrossPageReadingOrderAdjuster::pageNumber)
                .thenComparingInt(CrossPageReadingOrderAdjuster::tableRegionRowIndex);
    }

    private static List<StructuralBlock> renumber(List<StructuralBlock> blocks) {
        List<StructuralBlock> renumbered = new ArrayList<>(blocks.size());
        for (int order = 0; order < blocks.size(); order++) {
            StructuralBlock block = blocks.get(order);
            Map<String, Object> metadata = new HashMap<>(block.metadata());
            metadata.put("readingOrder", order);
            if (!metadata.containsKey("readingOrderSource")) {
                metadata.put("readingOrderSource", "cross-page-adjuster");
            }
            renumbered.add(new StructuralBlock(
                    block.blockType(),
                    block.level(),
                    block.content(),
                    order,
                    Map.copyOf(metadata)
            ));
        }
        return List.copyOf(renumbered);
    }

    private static Integer tableRegionId(StructuralBlock block) {
        Object raw = block.metadata().get("tableRegionId");
        if (raw instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private static int tableRegionRowIndex(StructuralBlock block) {
        Object raw = block.metadata().get("tableRegionRowIndex");
        if (raw instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    private static int pageNumber(StructuralBlock block) {
        Object page = block.metadata().get("pageNumber");
        if (page instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }
}
