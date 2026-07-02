package com.knowbase.ingestion.table;

import com.knowbase.ingestion.StructuralBlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Logical table grid: table → row → cell, derived from {@code table_row} blocks.
 */
public final class TableGridModel {

    private TableGridModel() {
    }

    public record Cell(
            int rowIndex,
            int columnIndex,
            String value,
            int rowSpan,
            int columnSpan,
            List<String> headerPath,
            List<Double> bbox
    ) {
    }

    public record Row(int rowIndex, String rowRole, List<Cell> cells) {
    }

    public record Grid(
            int tableRegionId,
            String tableRegionLabel,
            int rowCount,
            int columnCount,
            List<Double> regionBbox,
            List<Row> rows
    ) {
    }

    public static Map<Integer, Grid> buildByRegionId(List<StructuralBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return Map.of();
        }
        Map<Integer, List<StructuralBlock>> grouped = new LinkedHashMap<>();
        for (StructuralBlock block : blocks) {
            if (!"table_row".equals(block.blockType())) {
                continue;
            }
            Object raw = block.metadata().get("tableRegionId");
            if (!(raw instanceof Number number)) {
                continue;
            }
            grouped.computeIfAbsent(number.intValue(), ignored -> new ArrayList<>()).add(block);
        }
        Map<Integer, Grid> grids = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<StructuralBlock>> entry : grouped.entrySet()) {
            grids.put(entry.getKey(), fromTableRows(entry.getKey(), entry.getValue()));
        }
        return Map.copyOf(grids);
    }

    public static Grid fromTableRows(int tableRegionId, List<StructuralBlock> rows) {
        if (rows == null || rows.isEmpty()) {
            return new Grid(tableRegionId, "table-" + tableRegionId, 0, 0, List.of(), List.of());
        }
        String label = stringValue(rows.getFirst().metadata().get("tableRegionLabel"), "table-" + tableRegionId);
        List<Double> regionBbox = readBbox(rows.getFirst().metadata().get("tableRegionBbox"));
        if (regionBbox.isEmpty()) {
            regionBbox = readBbox(rows.getFirst().metadata().get("bbox"));
        }
        int maxColumnCount = 0;
        List<Row> gridRows = new ArrayList<>();
        for (StructuralBlock block : rows) {
            Map<String, Object> metadata = block.metadata();
            int rowIndex = intValue(metadata.get("tableRegionRowIndex"), gridRows.size());
            String rowRole = stringValue(metadata.get("rowRole"), "DATA");
            List<Cell> cells = cellsFromMetadata(metadata, rowIndex);
            if (cells.isEmpty()) {
                cells = cellsFromContent(block.content(), rowIndex, metadata);
            }
            maxColumnCount = Math.max(maxColumnCount, cells.size());
            gridRows.add(new Row(rowIndex, rowRole, List.copyOf(cells)));
        }
        return new Grid(
                tableRegionId,
                label,
                gridRows.size(),
                maxColumnCount,
                List.copyOf(regionBbox),
                List.copyOf(gridRows)
        );
    }

    public static Map<String, Object> toMetadata(Grid grid) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("tableRegionId", grid.tableRegionId());
        metadata.put("tableRegionLabel", grid.tableRegionLabel());
        metadata.put("tableGridRowCount", grid.rowCount());
        metadata.put("tableGridColumnCount", grid.columnCount());
        if (!grid.regionBbox().isEmpty()) {
            metadata.put("tableRegionBbox", grid.regionBbox());
        }
        List<Map<String, Object>> rowMaps = new ArrayList<>();
        for (Row row : grid.rows()) {
            Map<String, Object> rowMap = new HashMap<>();
            rowMap.put("rowIndex", row.rowIndex());
            rowMap.put("rowRole", row.rowRole());
            List<Map<String, Object>> cellMaps = new ArrayList<>();
            for (Cell cell : row.cells()) {
                Map<String, Object> cellMap = new HashMap<>();
                cellMap.put("rowIndex", cell.rowIndex());
                cellMap.put("columnIndex", cell.columnIndex());
                cellMap.put("value", cell.value());
                cellMap.put("rowSpan", cell.rowSpan());
                cellMap.put("columnSpan", cell.columnSpan());
                if (cell.headerPath() != null && !cell.headerPath().isEmpty()) {
                    cellMap.put("headerPath", cell.headerPath());
                }
                if (cell.bbox() != null && !cell.bbox().isEmpty()) {
                    cellMap.put("bbox", cell.bbox());
                }
                cellMaps.add(Map.copyOf(cellMap));
            }
            rowMap.put("cells", List.copyOf(cellMaps));
            rowMaps.add(Map.copyOf(rowMap));
        }
        metadata.put("tableGrid", Map.of(
                "tableRegionId", grid.tableRegionId(),
                "tableRegionLabel", grid.tableRegionLabel(),
                "rowCount", grid.rowCount(),
                "columnCount", grid.columnCount(),
                "rows", List.copyOf(rowMaps)
        ));
        return Map.copyOf(metadata);
    }

    @SuppressWarnings("unchecked")
    private static List<Cell> cellsFromMetadata(Map<String, Object> metadata, int rowIndex) {
        Object raw = metadata.get("cellCoordinates");
        if (!(raw instanceof List<?> coordinates)) {
            return List.of();
        }
        List<Cell> cells = new ArrayList<>();
        for (Object item : coordinates) {
            if (!(item instanceof Map<?, ?> cellMap)) {
                continue;
            }
            int columnIndex = intValue(cellMap.get("columnIndex"), cells.size());
            List<String> headerPath = cellMap.get("headerPath") instanceof List<?> path
                    ? path.stream().map(String::valueOf).toList()
                    : List.of();
            List<Double> bbox = readBbox(cellMap.get("bbox"));
            cells.add(new Cell(
                    intValue(cellMap.get("rowIndex"), rowIndex),
                    columnIndex,
                    stringValue(cellMap.get("value"), ""),
                    intValue(cellMap.get("rowSpan"), 1),
                    intValue(cellMap.get("columnSpan"), 1),
                    headerPath,
                    bbox
            ));
        }
        return cells;
    }

    private static List<Cell> cellsFromContent(String content, int rowIndex, Map<String, Object> metadata) {
        List<String> parts = splitCells(content);
        List<String> headerPath = metadata.get("headerPath") instanceof List<?> path
                ? path.stream().map(String::valueOf).toList()
                : List.of();
        List<Cell> cells = new ArrayList<>();
        for (int columnIndex = 0; columnIndex < parts.size(); columnIndex++) {
            List<String> cellHeaderPath = headerPath.size() > columnIndex
                    ? headerPath.subList(0, columnIndex + 1)
                    : List.of("Col" + (columnIndex + 1));
            cells.add(new Cell(
                    rowIndex,
                    columnIndex,
                    parts.get(columnIndex),
                    1,
                    1,
                    cellHeaderPath,
                    List.of()
            ));
        }
        return cells;
    }

    private static List<String> splitCells(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        String[] parts = content.split("\\|");
        List<String> cells = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isBlank()) {
                cells.add(trimmed);
            }
        }
        return cells;
    }

    @SuppressWarnings("unchecked")
    private static List<Double> readBbox(Object raw) {
        if (!(raw instanceof List<?> list) || list.size() < 4) {
            return List.of();
        }
        List<Double> bbox = new ArrayList<>(4);
        for (int index = 0; index < 4; index++) {
            Object value = list.get(index);
            if (value instanceof Number number) {
                bbox.add(number.doubleValue());
            } else {
                return List.of();
            }
        }
        return bbox;
    }

    private static int intValue(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private static String stringValue(Object value, String defaultValue) {
        return value == null || String.valueOf(value).isBlank() ? defaultValue : String.valueOf(value);
    }
}
