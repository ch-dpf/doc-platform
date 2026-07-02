package com.knowbase.ingestion.testsupport;

import com.knowbase.ingestion.ParsedDocument;
import com.knowbase.ingestion.StructuralBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class ParserOutputSnapshot {

    private ParserOutputSnapshot() {
    }

    public record Signature(
            int blockCount,
            List<String> rowRoles,
            List<Integer> tableRegionIds,
            Double parseConfidence
    ) {
    }

    public static Signature capture(ParsedDocument document) {
        List<String> rowRoles = new ArrayList<>();
        List<Integer> regionIds = new ArrayList<>();
        for (StructuralBlock block : document.blocks()) {
            Map<String, Object> metadata = block.metadata();
            if (metadata.containsKey("rowRole")) {
                rowRoles.add(String.valueOf(metadata.get("rowRole")));
            }
            if (metadata.containsKey("tableRegionId") && metadata.get("tableRegionId") instanceof Number number) {
                regionIds.add(number.intValue());
            }
        }
        Double parseConfidence = document.metadata() == null ? null : doubleMetadata(document.metadata().get("parseConfidence"));
        return new Signature(document.blocks().size(), List.copyOf(rowRoles), List.copyOf(regionIds), parseConfidence);
    }

    public static Map<String, Object> asMap(Signature signature) {
        Map<String, Object> map = new TreeMap<>();
        map.put("blockCount", signature.blockCount());
        map.put("rowRoles", signature.rowRoles());
        map.put("tableRegionIds", signature.tableRegionIds());
        map.put("parseConfidence", signature.parseConfidence());
        return map;
    }

    private static Double doubleMetadata(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return null;
    }
}
