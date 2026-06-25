package com.knowbase.ingestion.table;

import com.knowbase.ingestion.StructuralBlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TableParseConfidenceAggregator {

    private TableParseConfidenceAggregator() {
    }

    public static TableParseConfidence aggregate(List<StructuralBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return new TableParseConfidence(0.5d, List.of("no_structure_blocks"), 0, 0, 0, 0, 0);
        }
        int dataRows = 0;
        int headerRows = 0;
        int layoutRows = 0;
        int coordinateRows = 0;
        Set<Integer> regionIds = new HashSet<>();
        for (StructuralBlock block : blocks) {
            if (!"table_row".equals(block.blockType())) {
                continue;
            }
            Map<String, Object> metadata = block.metadata();
            String role = metadata == null ? "" : String.valueOf(metadata.getOrDefault("rowRole", ""));
            switch (role) {
                case "DATA" -> dataRows++;
                case "HEADER" -> headerRows++;
                case "LAYOUT", "SEPARATOR", "FORM_KV" -> layoutRows++;
                case "COORDINATE" -> coordinateRows++;
                default -> {
                }
            }
            Object regionId = metadata == null ? null : metadata.get("tableRegionId");
            if (regionId instanceof Number number) {
                regionIds.add(number.intValue());
            }
        }
        int totalRows = dataRows + headerRows + layoutRows + coordinateRows;
        double score = 1.0d;
        List<String> reasons = new ArrayList<>();
        if (totalRows == 0) {
            score = 0.5d;
            reasons.add("no_table_rows");
        } else {
            double dataRatio = (double) dataRows / totalRows;
            if (dataRatio < 0.3d) {
                score -= 0.2d;
                reasons.add("low_data_ratio");
            }
            double coordinateRatio = (double) coordinateRows / totalRows;
            if (coordinateRatio > 0.2d) {
                score -= 0.25d;
                reasons.add("high_coordinate_fallback");
            }
            if (headerRows == 0 && coordinateRows > 0) {
                score -= 0.15d;
                reasons.add("missing_header");
            }
        }
        return new TableParseConfidence(score, reasons, coordinateRows, dataRows, headerRows, layoutRows, regionIds.size());
    }

    public static Map<String, Object> toDocumentMetadata(TableParseConfidence confidence) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("parseConfidence", confidence.score());
        metadata.put("parseConfidenceSource", "table-adaptive");
        metadata.put("lowConfidenceReasons", confidence.reasons());
        metadata.put("tableRegionCount", confidence.tableRegionCount());
        metadata.put("parseStats", Map.of(
                "dataRows", confidence.dataRows(),
                "headerRows", confidence.headerRows(),
                "layoutRows", confidence.layoutRows(),
                "coordinateFallbackRows", confidence.coordinateFallbackRows()
        ));
        return Map.copyOf(metadata);
    }
}
