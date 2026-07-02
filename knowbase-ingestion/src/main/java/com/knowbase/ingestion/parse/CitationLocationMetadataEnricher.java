package com.knowbase.ingestion.parse;

import com.knowbase.ingestion.StructuralBlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Normalizes citation location fields on blocks (cellRef, primaryCellRef, word section hints).
 */
public final class CitationLocationMetadataEnricher {

    private CitationLocationMetadataEnricher() {
    }

    public static StructuralBlock apply(StructuralBlock block) {
        if (block == null || block.metadata() == null || block.metadata().isEmpty()) {
            return block;
        }
        Map<String, Object> metadata = block.metadata();
        Map<String, Object> merged = new HashMap<>(metadata);
        boolean changed = false;

        List<Map<String, Object>> cellCoordinates = cellCoordinates(metadata);
        if (!cellCoordinates.isEmpty()) {
            List<String> cellRefs = new ArrayList<>();
            for (Map<String, Object> cell : cellCoordinates) {
                String cellRef = stringValue(cell.get("cellRef"));
                if (cellRef == null || cellRef.isBlank()) {
                    cellRef = stringValue(cell.get("coordinate"));
                }
                if (cellRef != null && !cellRef.isBlank()) {
                    cellRefs.add(cellRef);
                }
            }
            if (!cellRefs.isEmpty()) {
                if (!merged.containsKey("cellRefs")) {
                    merged.put("cellRefs", List.copyOf(cellRefs));
                    changed = true;
                }
                if (!merged.containsKey("primaryCellRef")) {
                    merged.put("primaryCellRef", cellRefs.getFirst());
                    changed = true;
                }
            }
            if (!merged.containsKey("columnIndex")) {
                Object columnIndex = cellCoordinates.getFirst().get("columnIndex");
                if (columnIndex != null) {
                    merged.put("columnIndex", columnIndex);
                    changed = true;
                }
            }
        }

        if (metadata.get("headingPath") instanceof List<?> headingPath && !headingPath.isEmpty()) {
            if (!merged.containsKey("wordSectionPath")) {
                merged.put("wordSectionPath", headingPath);
                changed = true;
            }
        } else if (metadata.get("sectionPath") != null && !String.valueOf(metadata.get("sectionPath")).isBlank()) {
            if (!merged.containsKey("wordSectionPath")) {
                merged.put("wordSectionPath", List.of(String.valueOf(metadata.get("sectionPath")).trim()));
                changed = true;
            }
        }

        if (!changed) {
            return block;
        }
        return new StructuralBlock(block.blockType(), block.level(), block.content(), block.ordinal(), Map.copyOf(merged));
    }

    public static List<StructuralBlock> enrich(List<StructuralBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return blocks == null ? List.of() : blocks;
        }
        return blocks.stream().map(CitationLocationMetadataEnricher::apply).toList();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> cellCoordinates(Map<String, Object> metadata) {
        Object raw = metadata.get("cellCoordinates");
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> coordinates = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> cell = new HashMap<>();
                map.forEach((key, value) -> cell.put(String.valueOf(key), value));
                coordinates.add(Map.copyOf(cell));
            }
        }
        return List.copyOf(coordinates);
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }
}
