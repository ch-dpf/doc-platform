package com.knowbase.ingestion;

import com.knowbase.domain.status.ContentFamily;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StructuredTableDocumentParser implements DocumentParser {

    private final Tika tika = new Tika();
    private final DataFormatter formatter = new DataFormatter();

    @Override
    public boolean supports(String sourceUri, String mimeType) {
        String lowerMime = mimeType == null ? "" : mimeType.toLowerCase();
        if (lowerMime.contains("csv")
                || lowerMime.contains("spreadsheet")
                || lowerMime.contains("excel")
                || lowerMime.contains("sheet")) {
            return true;
        }
        String lowerUri = sourceUri == null ? "" : sourceUri.toLowerCase();
        return lowerUri.endsWith(".csv")
                || lowerUri.endsWith(".xls")
                || lowerUri.endsWith(".xlsx")
                || lowerUri.endsWith(".ods");
    }

    @Override
    public ParsedDocument parse(DocumentSource source) {
        String lowerUri = source.sourceUri() == null ? "" : source.sourceUri().toLowerCase();
        try {
            TableParseResult table;
            Map<String, Object> parsedMetadata = new HashMap<>(source.metadata());
            if (lowerUri.endsWith(".csv")) {
                table = parseCsv(new String(source.inputStream().readAllBytes(), StandardCharsets.UTF_8));
                parsedMetadata.put("tableFormat", "csv");
            } else {
                table = parseSpreadsheet(source);
                parsedMetadata.put("tableFormat", "spreadsheet");
            }
            String text = table.text();
            if (text == null || text.isBlank()) {
                Metadata metadata = new Metadata();
                text = tika.parseToString(source.inputStream(), metadata);
                parsedMetadata.put("fallbackParser", "tika");
                table = new TableParseResult(text, List.of());
            }
            parsedMetadata.put("parser", "table-deep");
            parsedMetadata.put("rowGroupCount", table.rowGroupCount());
            parsedMetadata.put("structureAware", !table.blocks().isEmpty());
            return new ParsedDocument(
                    source.sourceUri(),
                    firstNonBlank(source.filename(), source.sourceUri()),
                    text,
                    ContentFamily.STRUCTURED_TABLE,
                    parsedMetadata,
                    table.blocks()
            );
        } catch (IOException | TikaException exception) {
            throw new IllegalStateException("表格深度解析失败: " + source.sourceUri(), exception);
        }
    }

    private TableParseResult parseSpreadsheet(DocumentSource source) throws IOException {
        StringBuilder builder = new StringBuilder();
        List<StructuralBlock> blocks = new ArrayList<>();
        int ordinal = 0;
        try (Workbook workbook = WorkbookFactory.create(source.inputStream())) {
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                builder.append("# Sheet: ").append(sheet.getSheetName()).append('\n');
                List<CellRangeAddress> mergedRegions = sheet.getMergedRegions();
                int headerRowCount = detectHeaderRowCount(sheet, mergedRegions);
                List<List<String>> headerPaths = readHeaderPaths(sheet, headerRowCount, evaluator, mergedRegions);
                List<String> headers = flattenHeaderPaths(headerPaths);
                for (int rowIndex = sheet.getFirstRowNum() + headerRowCount; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) {
                        continue;
                    }
                    List<String> values = readRow(row, evaluator);
                    String rowText = formatRowGroup(headers, values);
                    if (rowText.isBlank()) {
                        continue;
                    }
                    builder.append(rowText).append('\n');
                    List<Map<String, Object>> mergedCells = mergedCellsForRow(mergedRegions, rowIndex);
                    Map<Integer, String> formulas = formulasForRow(row);
                    Map<String, Object> rowMetadata = new HashMap<>();
                    rowMetadata.put("tableFormat", "spreadsheet");
                    rowMetadata.put("sheetName", sheet.getSheetName());
                    rowMetadata.put("sheetIndex", sheetIndex);
                    rowMetadata.put("headerRowCount", headerRowCount);
                    rowMetadata.put("headerPathsByColumn", headerPaths);
                    rowMetadata.put("hiddenRow", row.getZeroHeight());
                    rowMetadata.put("hiddenColumns", hiddenColumns(sheet, Math.max(headers.size(), values.size())));
                    rowMetadata.put("formulaCells", formulas);
                    rowMetadata.put("crossSheetReferences", crossSheetReferences(formulas));
                    rowMetadata.put("mergedCells", mergedCells);
                    rowMetadata.put("hasMergedCells", !mergedCells.isEmpty());
                    blocks.add(tableRowBlock(rowText, ordinal++, rowIndex, rowIndex, headers, values, rowMetadata));
                }
                builder.append('\n');
            }
        }
        return new TableParseResult(builder.toString(), blocks);
    }

    private TableParseResult parseCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return new TableParseResult("", List.of());
        }
        String[] lines = csv.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        char delimiter = detectDelimiter(lines);
        List<String> headers = List.of();
        List<StructuralBlock> blocks = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        int ordinal = 0;
        for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            String line = lines[lineIndex];
            if (line == null || line.isBlank()) {
                continue;
            }
            List<String> values = parseDelimitedLine(line, delimiter);
            if (headers.isEmpty()) {
                headers = values;
                builder.append("# CSV Table").append('\n');
                continue;
            }
            String rowText = formatRowGroup(headers, values);
            if (rowText.isBlank()) {
                continue;
            }
            builder.append(rowText).append('\n');
            blocks.add(tableRowBlock(rowText, ordinal++, lineIndex, lineIndex, headers, values, Map.of("tableFormat", "csv")));
        }
        if (blocks.isEmpty()) {
            return new TableParseResult(csv, List.of(StructuralBlock.tableRow(csv.trim(), 0, 0)));
        }
        return new TableParseResult(builder.toString(), blocks);
    }

    private List<String> readRow(Row row) {
        return readRow(row, null);
    }

    private List<String> readRow(Row row, FormulaEvaluator evaluator) {
        if (row == null) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (int cellIndex = row.getFirstCellNum(); cellIndex < row.getLastCellNum(); cellIndex++) {
            Cell cell = row.getCell(cellIndex);
            values.add(cell == null ? "" : formatCell(cell, evaluator));
        }
        return values;
    }

    private String formatCell(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) {
            return "";
        }
        if (evaluator == null) {
            return formatter.formatCellValue(cell).trim();
        }
        return formatter.formatCellValue(cell, evaluator).trim();
    }

    private static String formatRowGroup(List<String> headers, List<String> values) {
        StringBuilder builder = new StringBuilder();
        int size = Math.max(headers.size(), values.size());
        for (int index = 0; index < size; index++) {
            String header = index < headers.size() ? headers.get(index) : "column_" + (index + 1);
            String value = index < values.size() ? values.get(index) : "";
            if (header.isBlank()) {
                header = "column_" + (index + 1);
            }
            if (!value.isBlank()) {
                if (!builder.isEmpty()) {
                    builder.append(" | ");
                }
                builder.append(header).append('=').append(value);
            }
        }
        return builder.toString();
    }

    private static StructuralBlock tableRowBlock(
            String content,
            int ordinal,
            int rowStart,
            int rowEnd,
            List<String> headers,
            List<String> values,
            Map<String, Object> extraMetadata
    ) {
        Map<String, Object> metadata = new HashMap<>(extraMetadata);
        int columnEnd = Math.max(headers.size(), values.size()) - 1;
        metadata.put("boundaryType", "table_row");
        metadata.put("rowIndex", rowStart);
        metadata.put("rowStart", rowStart);
        metadata.put("rowEnd", rowEnd);
        metadata.put("rowRange", rowStart == rowEnd ? String.valueOf(rowStart) : rowStart + ":" + rowEnd);
        metadata.put("columnStart", columnEnd < 0 ? 0 : 0);
        metadata.put("columnEnd", Math.max(0, columnEnd));
        metadata.put("columnRange", columnEnd < 0 ? "0" : "0:" + columnEnd);
        metadata.put("headerPath", headers.stream().filter(value -> value != null && !value.isBlank()).toList());
        metadata.put("cellCoordinates", cellCoordinates(rowStart, headers, values, metadata.get("mergedCells")));
        return new StructuralBlock("table_row", 0, content, ordinal, Map.copyOf(metadata));
    }

    private static int detectHeaderRowCount(Sheet sheet, List<CellRangeAddress> mergedRegions) {
        int first = sheet.getFirstRowNum();
        int count = 1;
        for (CellRangeAddress range : mergedRegions) {
            if (range.getFirstRow() == first && range.getLastRow() > first) {
                count = Math.max(count, range.getLastRow() - first + 1);
            }
        }
        Row firstRow = sheet.getRow(first);
        Row secondRow = sheet.getRow(first + 1);
        if (count == 1 && firstRow != null && secondRow != null
                && nonBlankCells(firstRow) < nonBlankCells(secondRow)
                && nonBlankCells(secondRow) > 1) {
            count = 2;
        }
        return Math.max(1, count);
    }

    private static int nonBlankCells(Row row) {
        int count = 0;
        if (row == null) {
            return 0;
        }
        for (int index = row.getFirstCellNum(); index < row.getLastCellNum(); index++) {
            Cell cell = row.getCell(index);
            if (cell != null && !new DataFormatter().formatCellValue(cell).isBlank()) {
                count++;
            }
        }
        return count;
    }

    private List<List<String>> readHeaderPaths(
            Sheet sheet,
            int headerRowCount,
            FormulaEvaluator evaluator,
            List<CellRangeAddress> mergedRegions
    ) {
        int first = sheet.getFirstRowNum();
        int maxColumns = 0;
        List<List<String>> rows = new ArrayList<>();
        for (int rowOffset = 0; rowOffset < headerRowCount; rowOffset++) {
            List<String> values = readRow(sheet.getRow(first + rowOffset), evaluator);
            rows.add(values);
            maxColumns = Math.max(maxColumns, values.size());
        }
        List<List<String>> paths = new ArrayList<>();
        for (int column = 0; column < maxColumns; column++) {
            List<String> path = new ArrayList<>();
            for (int rowOffset = 0; rowOffset < rows.size(); rowOffset++) {
                List<String> row = rows.get(rowOffset);
                String value = column < row.size() ? row.get(column) : "";
                if ((value == null || value.isBlank()) && !mergedRegions.isEmpty()) {
                    value = mergedHeaderValue(sheet, mergedRegions, first + rowOffset, column, evaluator);
                }
                if (value != null && !value.isBlank()) {
                    path.add(value);
                }
            }
            if (path.isEmpty()) {
                path.add("column_" + (column + 1));
            }
            paths.add(List.copyOf(path));
        }
        return paths;
    }

    private String mergedHeaderValue(
            Sheet sheet,
            List<CellRangeAddress> mergedRegions,
            int rowIndex,
            int columnIndex,
            FormulaEvaluator evaluator
    ) {
        for (CellRangeAddress range : mergedRegions) {
            if (rowIndex < range.getFirstRow() || rowIndex > range.getLastRow()
                    || columnIndex < range.getFirstColumn() || columnIndex > range.getLastColumn()) {
                continue;
            }
            Row row = sheet.getRow(range.getFirstRow());
            if (row == null) {
                return "";
            }
            Cell cell = row.getCell(range.getFirstColumn());
            return formatCell(cell, evaluator);
        }
        return "";
    }

    private static List<String> flattenHeaderPaths(List<List<String>> headerPaths) {
        return headerPaths.stream()
                .map(path -> String.join(" > ", path))
                .toList();
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
            Cell cell = row.getCell(index);
            if (cell != null && cell.getCellType() == org.apache.poi.ss.usermodel.CellType.FORMULA) {
                formulas.put(index, cell.getCellFormula());
            }
        }
        return formulas;
    }

    private static List<String> crossSheetReferences(Map<Integer, String> formulas) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?:'([^']+)'|([A-Za-z0-9_]+))!");
        List<String> references = new ArrayList<>();
        for (String formula : formulas.values()) {
            java.util.regex.Matcher matcher = pattern.matcher(formula);
            while (matcher.find()) {
                String sheet = matcher.group(1) == null ? matcher.group(2) : matcher.group(1);
                if (!references.contains(sheet)) {
                    references.add(sheet);
                }
            }
        }
        return references;
    }

    private static List<Map<String, Object>> cellCoordinates(
            int rowIndex,
            List<String> headers,
            List<String> values,
            Object mergedCells
    ) {
        List<Map<String, Object>> cells = new ArrayList<>();
        int size = Math.max(headers.size(), values.size());
        for (int columnIndex = 0; columnIndex < size; columnIndex++) {
            String header = columnIndex < headers.size() && headers.get(columnIndex) != null && !headers.get(columnIndex).isBlank()
                    ? headers.get(columnIndex)
                    : "column_" + (columnIndex + 1);
            String value = columnIndex < values.size() ? values.get(columnIndex) : "";
            Map<String, Object> cell = new HashMap<>();
            cell.put("rowIndex", rowIndex);
            cell.put("columnIndex", columnIndex);
            cell.put("coordinate", "R" + (rowIndex + 1) + "C" + (columnIndex + 1));
            cell.put("headerPath", header.contains(" > ") ? List.of(header.split(" > ")) : List.of(header));
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

    private static char detectDelimiter(String[] lines) {
        String sample = "";
        for (String line : lines) {
            if (line != null && !line.isBlank()) {
                sample = line;
                break;
            }
        }
        int comma = count(sample, ',');
        int tab = count(sample, '\t');
        int semicolon = count(sample, ';');
        if (tab >= comma && tab >= semicolon) {
            return '\t';
        }
        if (semicolon > comma) {
            return ';';
        }
        return ',';
    }

    private static int count(String value, char delimiter) {
        int count = 0;
        for (int index = 0; index < value.length(); index++) {
            if (value.charAt(index) == delimiter) {
                count++;
            }
        }
        return count;
    }

    private static List<String> parseDelimitedLine(String line, char delimiter) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char ch = line.charAt(index);
            if (ch == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
                continue;
            }
            if (ch == delimiter && !quoted) {
                values.add(current.toString().trim());
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        values.add(current.toString().trim());
        return values;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "untitled";
    }

    private record TableParseResult(String text, List<StructuralBlock> blocks) {
        private TableParseResult {
            text = text == null ? "" : text;
            blocks = blocks == null ? List.of() : List.copyOf(blocks);
        }

        private int rowGroupCount() {
            return blocks.isEmpty()
                    ? (int) text.lines().filter(line -> line.contains("=")).count()
                    : blocks.size();
        }
    }
}
