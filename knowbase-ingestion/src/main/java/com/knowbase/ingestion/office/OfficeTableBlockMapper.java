package com.knowbase.ingestion.office;

import com.knowbase.ingestion.StructuralBlock;
import com.knowbase.ingestion.adaptive.AdaptiveTableTextSerializer;
import com.knowbase.ingestion.adaptive.TableRowRole;
import com.knowbase.ingestion.table.MultiLevelHeaderStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class OfficeTableBlockMapper {

    private OfficeTableBlockMapper() {
    }

    public static List<StructuralBlock> fromDocxTable(DocxTableStructureExtractor.DocxTableModel model, int startOrdinal) {
        return mapRows(
                OfficeTableSource.DOCX,
                model.tableIndex(),
                model.rows().stream().map(row -> new GenericRow(
                        row.rowIndex(),
                        row.cells().stream()
                                .map(cell -> new GenericCell(cell.text(), cell.rowSpan(), cell.columnSpan()))
                                .toList(),
                        row.headerRow()
                )).toList(),
                startOrdinal
        );
    }

    public static List<StructuralBlock> fromHtmlTable(HtmlTableStructureExtractor.HtmlTableModel model, int startOrdinal) {
        return mapRows(
                OfficeTableSource.HTML,
                model.tableIndex(),
                model.rows().stream().map(row -> new GenericRow(
                        row.rowIndex(),
                        row.cells().stream()
                                .map(cell -> new GenericCell(cell.text(), cell.rowSpan(), cell.columnSpan()))
                                .toList(),
                        row.headerRow()
                )).toList(),
                startOrdinal,
                null
        );
    }

    public static List<StructuralBlock> fromPptxTable(
            PptxTableStructureExtractor.PptxTableModel model,
            int tableRegionId,
            int slideNumber,
            int startOrdinal
    ) {
        return mapRows(
                OfficeTableSource.PPTX,
                tableRegionId,
                model.rows().stream().map(row -> new GenericRow(
                        row.rowIndex(),
                        row.cells().stream()
                                .map(cell -> new GenericCell(cell.text(), 1, 1))
                                .toList(),
                        row.headerRow()
                )).toList(),
                startOrdinal,
                slideNumber
        );
    }

    private static List<StructuralBlock> mapRows(
            OfficeTableSource source,
            int tableIndex,
            List<GenericRow> rows,
            int startOrdinal
    ) {
        return mapRows(source, tableIndex, rows, startOrdinal, null);
    }

    private static List<StructuralBlock> mapRows(
            OfficeTableSource source,
            int tableIndex,
            List<GenericRow> rows,
            int startOrdinal,
            Integer slideNumber
    ) {
        MultiLevelHeaderStack headerStack = new MultiLevelHeaderStack();
        String[] activeHeaders = null;
        int columnCount = rows.stream().mapToInt(row -> row.cells().size()).max().orElse(0);
        List<StructuralBlock> blocks = new ArrayList<>();
        int ordinal = startOrdinal;
        for (GenericRow row : rows) {
            TableRowRole role = row.headerRow() ? TableRowRole.HEADER : TableRowRole.DATA;
            List<String> values = row.cells().stream().map(GenericCell::text).toList();
            if (role == TableRowRole.HEADER) {
                headerStack.pushHeaderRow(values);
                activeHeaders = headerStack.activeFlatHeaders(columnCount);
            }
            String sheetLabel = source.name().toLowerCase() + "-table-" + tableIndex;
            String content = AdaptiveTableTextSerializer.serialize(
                    role,
                    sheetLabel,
                    values,
                    activeHeaders,
                    headerStack,
                    columnCount
            );
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("tableFormat", source.name().toLowerCase());
            metadata.put("tableRegionId", tableIndex);
            metadata.put("tableRegionLabel", source.name().toLowerCase() + "-table-" + tableIndex);
            metadata.put("rowRole", role.name());
            metadata.put("rowIndex", row.rowIndex());
            metadata.put("boundaryType", "table_row");
            metadata.put("layoutRole", "table");
            if (slideNumber != null) {
                metadata.put("slideNumber", slideNumber);
                metadata.put("pageNumber", slideNumber);
            }
            metadata.put("indexableHint", AdaptiveTableTextSerializer.defaultIndexable(role));
            metadata.put("headerPath", headerStack.columnKeys(columnCount));
            metadata.put("cellCoordinates", buildCellCoordinates(row.rowIndex(), row.cells(), headerStack, columnCount));
            blocks.add(new StructuralBlock("table_row", 0, content, ordinal++, Map.copyOf(metadata)));
        }
        return blocks;
    }

    private static List<Map<String, Object>> buildCellCoordinates(
            int rowIndex,
            List<GenericCell> cells,
            MultiLevelHeaderStack headerStack,
            int columnCount
    ) {
        List<Map<String, Object>> coordinates = new ArrayList<>(cells.size());
        for (int columnIndex = 0; columnIndex < cells.size(); columnIndex++) {
            GenericCell cell = cells.get(columnIndex);
            List<String> headerPath = headerStack.headerPathForColumn(columnIndex, columnCount);
            Map<String, Object> coordinate = new HashMap<>();
            coordinate.put("rowIndex", rowIndex);
            coordinate.put("columnIndex", columnIndex);
            coordinate.put("coordinate", "R" + (rowIndex + 1) + "C" + (columnIndex + 1));
            coordinate.put("columnKey", headerPath.getLast());
            coordinate.put("headerPath", headerPath);
            coordinate.put("value", cell.text() == null ? "" : cell.text().trim());
            if (cell.rowSpan() > 1) {
                coordinate.put("rowSpan", cell.rowSpan());
            }
            if (cell.columnSpan() > 1) {
                coordinate.put("columnSpan", cell.columnSpan());
            }
            if (cell.rowSpan() > 1 || cell.columnSpan() > 1) {
                coordinate.put("merged", true);
            } else {
                coordinate.put("merged", false);
            }
            coordinates.add(Map.copyOf(coordinate));
        }
        return coordinates;
    }

    private record GenericCell(String text, int rowSpan, int columnSpan) {
    }

    private record GenericRow(int rowIndex, List<GenericCell> cells, boolean headerRow) {
    }
}
