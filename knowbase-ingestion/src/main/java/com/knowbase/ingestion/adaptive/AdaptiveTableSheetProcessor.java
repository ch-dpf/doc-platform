package com.knowbase.ingestion.adaptive;

import com.knowbase.ingestion.StructuralBlock;
import com.knowbase.ingestion.table.AdaptiveTableRegionContext;
import com.knowbase.ingestion.table.AdaptiveTableRegionDetector;
import com.knowbase.ingestion.table.MultiLevelHeaderStack;
import com.knowbase.ingestion.table.TableCellMetadataBuilder;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;

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
                startOrdinal,
                evaluator
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
        return processRows(sheetLabel, 0, "csv", columnCount, sheetRows, null, List.of(), startOrdinal, null);
    }

    private static SheetParseResult processRows(
            String sheetLabel,
            int sheetIndex,
            String tableFormat,
            int columnCount,
            List<SheetRow> sheetRows,
            Sheet sheet,
            List<CellRangeAddress> mergedRegions,
            int startOrdinal,
            FormulaEvaluator evaluator
    ) {
        StringBuilder textBuilder = new StringBuilder();
        List<StructuralBlock> blocks = new ArrayList<>();
        String[] activeColumnHeaders = null;
        MultiLevelHeaderStack headerStack = new MultiLevelHeaderStack();
        AdaptiveTableRegionContext regionContext = new AdaptiveTableRegionContext();
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

            if (AdaptiveTableRegionDetector.shouldStartRegion(role, sheetRow.values(), sheetRow.populatedCount())) {
                regionContext.startRegion(sheetRow.rowIndex(), AdaptiveTableRegionDetector.regionLabel(role, sheetRow.values()));
            }

            if (role == TableRowRole.LAYOUT || role == TableRowRole.SEPARATOR || role == TableRowRole.FORM_KV) {
                activeColumnHeaders = null;
                headerStack.reset();
            }
            if (role == TableRowRole.HEADER) {
                headerStack.pushHeaderRow(sheetRow.values());
                activeColumnHeaders = headerStack.activeFlatHeaders(columnCount);
            }

            String rowText = AdaptiveTableTextSerializer.serialize(
                    role,
                    sheetLabel,
                    sheetRow.values(),
                    activeColumnHeaders,
                    headerStack,
                    columnCount
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
                    activeColumnHeaders,
                    headerStack,
                    regionContext,
                    evaluator
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
            String[] columnHeaders,
            MultiLevelHeaderStack headerStack,
            AdaptiveTableRegionContext regionContext,
            FormulaEvaluator evaluator
    ) {
        int rowIndex = sheetRow.rowIndex();
        List<String> columnKeys = flatColumnKeys(columnHeaders, headerStack, columnCount);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("tableFormat", tableFormat);
        metadata.put("sheetName", sheet == null ? "CSV" : sheet.getSheetName());
        metadata.put("sheetIndex", sheetIndex);
        metadata.put("rowRole", role.name());
        metadata.put("serializationStrategy", strategyName(role));
        metadata.put("indexableHint", AdaptiveTableTextSerializer.defaultIndexable(role));
        metadata.put("columnKeys", columnKeys);
        metadata.put("headerRowCount", headerStack.headerRowCount());
        metadata.put("tableRegionId", regionContext.currentRegionId());
        if (!regionContext.currentRegionLabel().isBlank()) {
            metadata.put("tableRegionLabel", regionContext.currentRegionLabel());
        }
        metadata.put("boundaryType", "table_row");
        metadata.put("rowIndex", rowIndex);
        metadata.put("rowStart", rowIndex);
        metadata.put("rowEnd", rowIndex);
        metadata.put("rowRange", String.valueOf(rowIndex));
        metadata.put("columnStart", 0);
        metadata.put("columnEnd", Math.max(0, columnCount - 1));
        metadata.put("columnRange", columnCount <= 1 ? "0" : "0:" + (columnCount - 1));
        metadata.put("headerPath", columnKeys);
        Map<Integer, String> formulas = Map.of();
        Map<Integer, String> computedValues = Map.of();
        List<Map<String, Object>> mergedCells = List.of();
        if (sheetRow.row() != null && sheet != null) {
            metadata.put("hiddenRow", sheetRow.row().getZeroHeight());
            metadata.put("hiddenColumns", hiddenColumns(sheet, columnCount));
            formulas = formulasForRow(sheetRow.row());
            computedValues = computedValuesForRow(sheetRow.row(), evaluator);
            metadata.put("formulaCells", formulas);
            metadata.put("crossSheetReferences", crossSheetReferences(formulas));
            mergedCells = mergedCellsForRow(mergedRegions, rowIndex);
            metadata.put("mergedCells", mergedCells);
            metadata.put("hasMergedCells", !mergedCells.isEmpty());
        } else {
            metadata.put("mergedCells", List.of());
            metadata.put("hasMergedCells", false);
        }
        metadata.put(
                "cellCoordinates",
                TableCellMetadataBuilder.buildWithHeaderPaths(
                        rowIndex,
                        sheetRow.values(),
                        headerStack,
                        columnCount,
                        mergedCells,
                        formulas,
                        computedValues
                )
        );
        return new StructuralBlock("table_row", 0, content, ordinal, Map.copyOf(metadata));
    }

    private static List<String> flatColumnKeys(String[] columnHeaders, MultiLevelHeaderStack headerStack, int columnCount) {
        if (headerStack.headerRowCount() > 0) {
            String[] flat = headerStack.activeFlatHeaders(columnCount);
            List<String> keys = new ArrayList<>(flat.length);
            for (String key : flat) {
                keys.add(key);
            }
            return keys;
        }
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
        return headerStack.columnKeys(columnCount);
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

    private static Map<Integer, String> computedValuesForRow(Row row, FormulaEvaluator evaluator) {
        Map<Integer, String> computed = new HashMap<>();
        if (row == null) {
            return computed;
        }
        for (int index = row.getFirstCellNum(); index < row.getLastCellNum(); index++) {
            Cell cell = row.getCell(index);
            if (cell == null) {
                continue;
            }
            if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.FORMULA && evaluator != null) {
                computed.put(index, new org.apache.poi.ss.usermodel.DataFormatter().formatCellValue(cell, evaluator).trim());
            }
        }
        return computed;
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
}
