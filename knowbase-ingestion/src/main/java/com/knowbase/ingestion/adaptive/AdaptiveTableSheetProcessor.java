package com.knowbase.ingestion.adaptive;

import com.knowbase.ingestion.StructuralBlock;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adaptive Excel parse pipeline for one worksheet: physical read → layout roles → serialized text.
 */
public final class AdaptiveTableSheetProcessor {

    private AdaptiveTableSheetProcessor() {
    }

    public record SheetParseResult(String sheetText, List<StructuralBlock> blocks) {
    }

    public static SheetParseResult process(
            Sheet sheet,
            int sheetIndex,
            int columnCount,
            FormulaEvaluator evaluator,
            List<CellRangeAddress> mergedRegions,
            RowValueReader rowValueReader,
            int startOrdinal
    ) {
        List<SheetRow> sheetRows = readSheetRows(sheet, columnCount, evaluator, mergedRegions, rowValueReader);
        return processRows(
                sheet.getSheetName(),
                sheetIndex,
                "spreadsheet",
                columnCount,
                sheetRows,
                sheet,
                mergedRegions,
                startOrdinal
        );
    }

    public static SheetParseResult processCsvRows(
            String sheetLabel,
            List<List<String>> rowValues,
            int startOrdinal
    ) {
        List<SheetRow> sheetRows = new ArrayList<>(rowValues.size());
        for (int rowIndex = 0; rowIndex < rowValues.size(); rowIndex++) {
            List<String> values = rowValues.get(rowIndex);
            sheetRows.add(new SheetRow(rowIndex, values, countPopulated(values), null));
        }
        int columnCount = rowValues.stream().mapToInt(List::size).max().orElse(0);
        return processRows(sheetLabel, 0, "csv", columnCount, sheetRows, null, List.of(), startOrdinal);
    }

    private static SheetParseResult processRows(
            String sheetLabel,
            int sheetIndex,
            String tableFormat,
            int columnCount,
            List<SheetRow> sheetRows,
            Sheet sheet,
            List<CellRangeAddress> mergedRegions,
            int startOrdinal
    ) {
        StringBuilder textBuilder = new StringBuilder();
        List<StructuralBlock> blocks = new ArrayList<>();
        String[] activeColumnHeaders = null;
        int ordinal = startOrdinal;

        textBuilder.append("# Sheet: ").append(sheetLabel).append('\n');

        for (int rowPointer = 0; rowPointer < sheetRows.size(); rowPointer++) {
            SheetRow sheetRow = sheetRows.get(rowPointer);
            if (sheetRow.populatedCount() == 0) {
                continue;
            }
            List<String> nextValues = rowPointer + 1 < sheetRows.size()
                    ? sheetRows.get(rowPointer + 1).values()
                    : List.of();

            TableRowRole role = AdaptiveTableLayoutAnalyzer.detectRole(
                    sheetRow.values(),
                    nextValues,
                    activeColumnHeaders,
                    sheetRow.populatedCount(),
                    RowLayoutContext.of(columnCount, horizontalMergeSpan(mergedRegions, sheetRow.rowIndex()))
            );

            if (role == TableRowRole.LAYOUT || role == TableRowRole.SEPARATOR || role == TableRowRole.FORM_KV) {
                activeColumnHeaders = null;
            }
            if (role == TableRowRole.HEADER) {
                activeColumnHeaders = AdaptiveTableLayoutAnalyzer.buildColumnHeaders(sheetRow.values());
            }

            String rowText = AdaptiveTableTextSerializer.serialize(
                    role,
                    sheetLabel,
                    sheetRow.values(),
                    activeColumnHeaders
            );
            if (rowText.isBlank()) {
                continue;
            }

            textBuilder.append(rowText).append('\n');
            blocks.add(toBlock(
                    rowText,
                    ordinal++,
                    sheetRow,
                    sheet,
                    sheetIndex,
                    tableFormat,
                    columnCount,
                    mergedRegions,
                    role,
                    activeColumnHeaders
            ));
        }

        textBuilder.append('\n');
        return new SheetParseResult(textBuilder.toString(), blocks);
    }

    private static int horizontalMergeSpan(List<CellRangeAddress> mergedRegions, int rowIndex) {
        if (mergedRegions == null || mergedRegions.isEmpty()) {
            return 0;
        }
        int maxSpan = 0;
        for (CellRangeAddress range : mergedRegions) {
            if (rowIndex < range.getFirstRow() || rowIndex > range.getLastRow()) {
                continue;
            }
            maxSpan = Math.max(maxSpan, range.getLastColumn() - range.getFirstColumn() + 1);
        }
        return maxSpan;
    }

    private static List<SheetRow> readSheetRows(
            Sheet sheet,
            int columnCount,
            FormulaEvaluator evaluator,
            List<CellRangeAddress> mergedRegions,
            RowValueReader rowValueReader
    ) {
        List<SheetRow> rows = new ArrayList<>();
        for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                rows.add(new SheetRow(rowIndex, List.of(), 0, row));
                continue;
            }
            List<String> values = rowValueReader.readRowValues(sheet, rowIndex, columnCount, evaluator, mergedRegions);
            rows.add(new SheetRow(rowIndex, values, countPopulated(values), row));
        }
        return rows;
    }

    private static StructuralBlock toBlock(
            String content,
            int ordinal,
            SheetRow sheetRow,
            Sheet sheet,
            int sheetIndex,
            String tableFormat,
            int columnCount,
            List<CellRangeAddress> mergedRegions,
            TableRowRole role,
            String[] columnHeaders
    ) {
        int rowIndex = sheetRow.rowIndex();
        List<String> columnKeys = headerPath(columnHeaders, columnCount);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("tableFormat", tableFormat);
        metadata.put("sheetName", sheet == null ? "CSV" : sheet.getSheetName());
        metadata.put("sheetIndex", sheetIndex);
        metadata.put("rowRole", role.name());
        metadata.put("serializationStrategy", strategyName(role));
        metadata.put("indexableHint", AdaptiveTableTextSerializer.defaultIndexable(role));
        metadata.put("columnKeys", columnKeys);
        metadata.put("headerRowCount", role == TableRowRole.HEADER ? 1 : 0);
        metadata.put("boundaryType", "table_row");
        metadata.put("rowIndex", rowIndex);
        metadata.put("rowStart", rowIndex);
        metadata.put("rowEnd", rowIndex);
        metadata.put("rowRange", String.valueOf(rowIndex));
        metadata.put("columnStart", 0);
        metadata.put("columnEnd", Math.max(0, columnCount - 1));
        metadata.put("columnRange", columnCount <= 1 ? "0" : "0:" + (columnCount - 1));
        metadata.put("headerPath", columnKeys);
        if (sheetRow.row() != null && sheet != null) {
            metadata.put("hiddenRow", sheetRow.row().getZeroHeight());
            metadata.put("hiddenColumns", hiddenColumns(sheet, columnCount));
            Map<Integer, String> formulas = formulasForRow(sheetRow.row());
            metadata.put("formulaCells", formulas);
            metadata.put("crossSheetReferences", crossSheetReferences(formulas));
            metadata.put("mergedCells", mergedCellsForRow(mergedRegions, rowIndex));
            metadata.put("hasMergedCells", !((List<?>) metadata.get("mergedCells")).isEmpty());
        } else {
            metadata.put("mergedCells", List.of());
            metadata.put("hasMergedCells", false);
        }
        metadata.put("cellCoordinates", cellCoordinates(rowIndex, columnKeys, sheetRow.values(), metadata.get("mergedCells")));
        return new StructuralBlock("table_row", 0, content, ordinal, Map.copyOf(metadata));
    }

    private static String strategyName(TableRowRole role) {
        return switch (role) {
            case LAYOUT -> "layout";
            case FORM_KV -> "form_kv";
            case SEPARATOR -> "separator";
            case HEADER -> "tabular_header";
            case DATA -> "tabular";
            case COORDINATE -> "coordinate_fallback";
        };
    }

    private static List<String> headerPath(String[] columnHeaders, int columnCount) {
        if (columnHeaders != null && columnHeaders.length > 0) {
            List<String> headers = new ArrayList<>();
            for (String header : columnHeaders) {
                if (header != null && !header.isBlank()) {
                    headers.add(header.trim());
                }
            }
            if (!headers.isEmpty()) {
                return headers;
            }
        }
        return columnKeys(columnCount);
    }

    private static List<String> columnKeys(int columnCount) {
        List<String> keys = new ArrayList<>(columnCount);
        for (int index = 0; index < columnCount; index++) {
            keys.add(CellReference.convertNumToColString(index));
        }
        return keys;
    }

    private static int countPopulated(List<String> values) {
        int count = 0;
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                count++;
            }
        }
        return count;
    }

    @FunctionalInterface
    public interface RowValueReader {
        List<String> readRowValues(
                Sheet sheet,
                int rowIndex,
                int columnCount,
                FormulaEvaluator evaluator,
                List<CellRangeAddress> mergedRegions
        );
    }

    private record SheetRow(int rowIndex, List<String> values, int populatedCount, Row row) {
    }

    private static List<Integer> hiddenColumns(Sheet sheet, int columnCount) {
        List<Integer> hidden = new ArrayList<>();
        for (int column = 0; column < columnCount; column++) {
            if (sheet.isColumnHidden(column)) {
                hidden.add(column);
            }
        }
        return hidden;
    }

    private static Map<Integer, String> formulasForRow(Row row) {
        Map<Integer, String> formulas = new HashMap<>();
        for (int index = row.getFirstCellNum(); index < row.getLastCellNum(); index++) {
            var cell = row.getCell(index);
            if (cell != null && cell.getCellType() == org.apache.poi.ss.usermodel.CellType.FORMULA) {
                formulas.put(index, cell.getCellFormula());
            }
        }
        return formulas;
    }

    private static List<String> crossSheetReferences(Map<Integer, String> formulas) {
        if (formulas == null || formulas.isEmpty()) {
            return List.of();
        }
        var pattern = java.util.regex.Pattern.compile("(?:'([^']+)'|([A-Za-z0-9_]+))!");
        List<String> references = new ArrayList<>();
        for (String formula : formulas.values()) {
            var matcher = pattern.matcher(formula);
            while (matcher.find()) {
                String sheet = matcher.group(1) == null ? matcher.group(2) : matcher.group(1);
                if (!references.contains(sheet)) {
                    references.add(sheet);
                }
            }
        }
        return references;
    }

    private static List<Map<String, Object>> mergedCellsForRow(List<CellRangeAddress> mergedRegions, int rowIndex) {
        if (mergedRegions == null || mergedRegions.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> ranges = new ArrayList<>();
        for (CellRangeAddress range : mergedRegions) {
            if (rowIndex < range.getFirstRow() || rowIndex > range.getLastRow()) {
                continue;
            }
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("rowStart", range.getFirstRow());
            metadata.put("rowEnd", range.getLastRow());
            metadata.put("columnStart", range.getFirstColumn());
            metadata.put("columnEnd", range.getLastColumn());
            metadata.put("rowSpan", range.getLastRow() - range.getFirstRow() + 1);
            metadata.put("columnSpan", range.getLastColumn() - range.getFirstColumn() + 1);
            metadata.put("range", "R" + (range.getFirstRow() + 1) + "C" + (range.getFirstColumn() + 1)
                    + ":R" + (range.getLastRow() + 1) + "C" + (range.getLastColumn() + 1));
            ranges.add(Map.copyOf(metadata));
        }
        return ranges;
    }

    private static List<Map<String, Object>> cellCoordinates(
            int rowIndex,
            List<String> columnKeys,
            List<String> values,
            Object mergedCells
    ) {
        List<Map<String, Object>> cells = new ArrayList<>();
        int size = Math.max(columnKeys.size(), values.size());
        for (int columnIndex = 0; columnIndex < size; columnIndex++) {
            String columnKey = columnIndex < columnKeys.size() && columnKeys.get(columnIndex) != null
                    && !columnKeys.get(columnIndex).isBlank()
                    ? columnKeys.get(columnIndex)
                    : CellReference.convertNumToColString(columnIndex);
            String value = columnIndex < values.size() ? values.get(columnIndex) : "";
            Map<String, Object> cell = new HashMap<>();
            cell.put("rowIndex", rowIndex);
            cell.put("columnIndex", columnIndex);
            cell.put("coordinate", "R" + (rowIndex + 1) + "C" + (columnIndex + 1));
            cell.put("columnKey", columnKey);
            cell.put("headerPath", List.of(columnKey));
            cell.put("value", value == null ? "" : value);
            Map<String, Object> merged = mergedMetadataForCell(mergedCells, rowIndex, columnIndex);
            if (!merged.isEmpty()) {
                cell.put("merged", true);
                cell.put("mergedRange", merged.get("range"));
                cell.put("rowSpan", merged.get("rowSpan"));
                cell.put("columnSpan", merged.get("columnSpan"));
            } else {
                cell.put("merged", false);
            }
            cells.add(Map.copyOf(cell));
        }
        return cells;
    }

    private static Map<String, Object> mergedMetadataForCell(Object mergedCells, int rowIndex, int columnIndex) {
        if (!(mergedCells instanceof List<?> ranges)) {
            return Map.of();
        }
        for (Object item : ranges) {
            if (!(item instanceof Map<?, ?> range)) {
                continue;
            }
            int rowStart = intValue(range.get("rowStart"), -1);
            int rowEnd = intValue(range.get("rowEnd"), -1);
            int columnStart = intValue(range.get("columnStart"), -1);
            int columnEnd = intValue(range.get("columnEnd"), -1);
            if (rowStart <= rowIndex && rowIndex <= rowEnd && columnStart <= columnIndex && columnIndex <= columnEnd) {
                Map<String, Object> metadata = new HashMap<>();
                for (Map.Entry<?, ?> entry : range.entrySet()) {
                    metadata.put(String.valueOf(entry.getKey()), entry.getValue());
                }
                return metadata;
            }
        }
        return Map.of();
    }

    private static int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }
}
