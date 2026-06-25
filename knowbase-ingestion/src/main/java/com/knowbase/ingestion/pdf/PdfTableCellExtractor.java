package com.knowbase.ingestion.pdf;

import com.knowbase.ingestion.StructuralBlock;
import com.knowbase.ingestion.adaptive.AdaptiveTableTextSerializer;
import com.knowbase.ingestion.table.MultiLevelHeaderStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PdfTableCellExtractor {

    private PdfTableCellExtractor() {
    }

    public static List<StructuralBlock> toStructuralBlocks(
            List<PdfTableRowInput> rows,
            int tableRegionId,
            int startOrdinal
    ) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        int columnCount = PdfTableColumnDetector.estimateColumnCount(rows);
        List<PdfTableColumnDetector.ColumnBoundary> columnBoundaries = PdfTableColumnDetector.detectFromBlocks(rows);
        List<Double> regionBbox = PdfTableCellBboxAssigner.tableRegionBbox(rows);
        int firstPage = rows.getFirst().pageNumber();
        String tableRegionLabel = "pdf-table-" + tableRegionId;
        MultiLevelHeaderStack headerStack = new MultiLevelHeaderStack();
        List<String> headerCells = PdfTableColumnDetector.splitCells(rows.getFirst().content());
        if (looksLikeHeader(headerCells)) {
            headerStack.pushHeaderRow(headerCells);
        }
        String[] activeHeaders = headerStack.headerRowCount() > 0
                ? headerStack.activeFlatHeaders(columnCount)
                : null;

        List<StructuralBlock> blocks = new ArrayList<>();
        int dataStart = headerStack.headerRowCount() > 0 ? 1 : 0;
        for (int rowIndex = dataStart; rowIndex < rows.size(); rowIndex++) {
            PdfTableRowInput row = rows.get(rowIndex);
            List<String> values = new ArrayList<>(PdfTableColumnDetector.splitAlignedCells(row.content(), columnCount));
            String content = AdaptiveTableTextSerializer.serialize(
                    com.knowbase.ingestion.adaptive.TableRowRole.DATA,
                    "PDF",
                    values,
                    activeHeaders,
                    headerStack,
                    columnCount
            );
            if (content.isBlank()) {
                content = row.content().replaceAll("\\s{2,}|\\t+", " | ");
            }
            Map<String, Object> metadata = baseMetadata(row, tableRegionId, tableRegionLabel, firstPage, rowIndex, rows.size());
            metadata.put("rowRole", "DATA");
            metadata.put("tableFormat", "pdf");
            metadata.put("indexableHint", true);
            metadata.put("headerPath", headerStack.headerRowCount() > 0
                    ? headerStack.columnKeys(columnCount)
                    : columnBoundaries.stream().map(b -> "Col" + (b.index() + 1)).toList());
            metadata.put("rowRange", PdfTableCellBboxAssigner.rowRange(rowIndex));
            metadata.put("columnRange", PdfTableCellBboxAssigner.columnRange(columnCount));
            if (!regionBbox.isEmpty()) {
                metadata.put("tableRegionBbox", regionBbox);
            }
            metadata.put("cellCoordinates", buildCellCoordinates(row, rowIndex, values, headerStack, columnCount, columnBoundaries));
            blocks.add(new StructuralBlock("table_row", 0, content, startOrdinal + rowIndex - dataStart, Map.copyOf(metadata)));
        }
        if (blocks.isEmpty() && headerStack.headerRowCount() > 0) {
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                PdfTableRowInput row = rows.get(rowIndex);
                Map<String, Object> metadata = baseMetadata(row, tableRegionId, tableRegionLabel, firstPage, rowIndex, rows.size());
                metadata.put("rowRole", rowIndex == 0 ? "HEADER" : "DATA");
                metadata.put("tableFormat", "pdf");
                metadata.put("indexableHint", rowIndex != 0);
                blocks.add(new StructuralBlock(
                        "table_row",
                        0,
                        row.content().replaceAll("\\s{2,}|\\t+", " | "),
                        startOrdinal + rowIndex,
                        Map.copyOf(metadata)
                ));
            }
        }
        return blocks;
    }

    private static boolean looksLikeHeader(List<String> cells) {
        if (cells.size() < 2) {
            return false;
        }
        long alpha = cells.stream().filter(PdfTableCellExtractor::isMostlyLabel).count();
        return alpha >= Math.max(1, cells.size() / 2);
    }

    private static boolean isMostlyLabel(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return !value.matches("^[\\d.,]+$");
    }

    private static Map<String, Object> baseMetadata(
            PdfTableRowInput row,
            int tableRegionId,
            String tableRegionLabel,
            int firstPage,
            int rowIndex,
            int rowCount
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("boundaryType", "table_row");
        metadata.put("layoutRole", "table");
        metadata.put("pageNumber", row.pageNumber());
        metadata.put("readingOrder", row.readingOrder());
        metadata.put("columnIndex", row.columnIndex());
        metadata.put("columnCount", row.columnCount());
        metadata.put("bbox", List.of(round(row.minX()), round(row.y()), round(row.width()), round(row.height())));
        metadata.put("tableRegionId", tableRegionId);
        metadata.put("tableRegionLabel", tableRegionLabel);
        metadata.put("tableDetection", row.cellBoundaryX().size() >= 2 ? "aligned-column" : "stream");
        if (row.pageNumber() > firstPage) {
            metadata.put("tableContinuation", true);
        }
        metadata.put("tableRegionRowIndex", rowIndex);
        metadata.put("tableRegionRowCount", rowCount);
        metadata.put("layoutParsing", true);
        return metadata;
    }

    private static List<Map<String, Object>> buildCellCoordinates(
            PdfTableRowInput row,
            int rowIndex,
            List<String> values,
            MultiLevelHeaderStack headerStack,
            int columnCount,
            List<PdfTableColumnDetector.ColumnBoundary> columnBoundaries
    ) {
        List<Map<String, Object>> cells = new ArrayList<>();
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            List<String> path = headerStack.headerPathForColumn(columnIndex, columnCount);
            Map<String, Object> cell = new HashMap<>();
            cell.put("rowIndex", rowIndex);
            cell.put("columnIndex", columnIndex);
            cell.put("columnKey", path.getLast());
            cell.put("headerPath", path);
            cell.put("value", columnIndex < values.size() ? values.get(columnIndex) : "");
            cell.put("merged", false);
            List<Double> bbox = PdfTableCellBboxAssigner.cellBbox(row, columnIndex, columnCount, columnBoundaries);
            if (!bbox.isEmpty()) {
                cell.put("bbox", bbox);
                cell.put("bboxSource", "pdf-table-estimate");
            }
            cells.add(Map.copyOf(cell));
        }
        return cells;
    }

    private static double round(float value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
