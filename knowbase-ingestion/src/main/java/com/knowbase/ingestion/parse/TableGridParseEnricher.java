package com.knowbase.ingestion.parse;

import com.knowbase.ingestion.StructuralBlock;
import com.knowbase.ingestion.table.TableGridModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Attaches {@code tableGrid} metadata to the first block of each table region.
 */
public final class TableGridParseEnricher {

    private TableGridParseEnricher() {
    }

    public static List<StructuralBlock> enrich(List<StructuralBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return blocks;
        }
        Map<Integer, TableGridModel.Grid> grids = TableGridModel.buildByRegionId(blocks);
        if (grids.isEmpty()) {
            return blocks;
        }
        Set<Integer> stampedRegions = new HashSet<>();
        List<StructuralBlock> enriched = new ArrayList<>(blocks.size());
        for (StructuralBlock block : blocks) {
            if (!"table_row".equals(block.blockType())) {
                enriched.add(block);
                continue;
            }
            Object rawRegionId = block.metadata().get("tableRegionId");
            if (!(rawRegionId instanceof Number regionIdNumber)) {
                enriched.add(block);
                continue;
            }
            int regionId = regionIdNumber.intValue();
            TableGridModel.Grid grid = grids.get(regionId);
            if (grid == null || stampedRegions.contains(regionId)) {
                enriched.add(block);
                continue;
            }
            Map<String, Object> metadata = new HashMap<>(block.metadata());
            metadata.putAll(TableGridModel.toMetadata(grid));
            enriched.add(new StructuralBlock(
                    block.blockType(),
                    block.level(),
                    block.content(),
                    block.ordinal(),
                    Map.copyOf(metadata)
            ));
            stampedRegions.add(regionId);
        }
        return enriched;
    }
}
