package com.knowbase.ingestion;

import com.knowbase.domain.status.ContentFamily;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
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
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                builder.append("# Sheet: ").append(sheet.getSheetName()).append('\n');
                List<String> headers = readRow(sheet.getRow(sheet.getFirstRowNum()));
                for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) {
                        continue;
                    }
                    List<String> values = readRow(row);
                    String rowText = formatRowGroup(headers, values);
                    if (rowText.isBlank()) {
                        continue;
                    }
                    builder.append(rowText).append('\n');
                    blocks.add(tableRowBlock(rowText, ordinal++, rowIndex, Map.of(
                            "tableFormat", "spreadsheet",
                            "sheetName", sheet.getSheetName(),
                            "sheetIndex", sheetIndex
                    )));
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
            blocks.add(tableRowBlock(rowText, ordinal++, lineIndex, Map.of("tableFormat", "csv")));
        }
        if (blocks.isEmpty()) {
            return new TableParseResult(csv, List.of(StructuralBlock.tableRow(csv.trim(), 0, 0)));
        }
        return new TableParseResult(builder.toString(), blocks);
    }

    private List<String> readRow(Row row) {
        if (row == null) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (int cellIndex = row.getFirstCellNum(); cellIndex < row.getLastCellNum(); cellIndex++) {
            Cell cell = row.getCell(cellIndex);
            values.add(cell == null ? "" : formatter.formatCellValue(cell).trim());
        }
        return values;
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

    private static StructuralBlock tableRowBlock(String content, int ordinal, int rowIndex, Map<String, Object> extraMetadata) {
        Map<String, Object> metadata = new HashMap<>(extraMetadata);
        metadata.put("boundaryType", "table_row");
        metadata.put("rowIndex", rowIndex);
        return new StructuralBlock("table_row", 0, content, ordinal, Map.copyOf(metadata));
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
