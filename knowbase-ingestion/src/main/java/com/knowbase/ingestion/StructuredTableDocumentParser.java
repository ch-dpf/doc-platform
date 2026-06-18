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
            String text;
            Map<String, Object> parsedMetadata = new HashMap<>(source.metadata());
            if (lowerUri.endsWith(".csv")) {
                text = new String(source.inputStream().readAllBytes(), StandardCharsets.UTF_8);
                parsedMetadata.put("tableFormat", "csv");
            } else {
                text = parseSpreadsheet(source);
                parsedMetadata.put("tableFormat", "spreadsheet");
            }
            if (text == null || text.isBlank()) {
                Metadata metadata = new Metadata();
                text = tika.parseToString(source.inputStream(), metadata);
                parsedMetadata.put("fallbackParser", "tika");
            }
            parsedMetadata.put("parser", "table-deep");
            parsedMetadata.put("rowGroupCount", countRowGroups(text));
            return new ParsedDocument(
                    source.sourceUri(),
                    firstNonBlank(source.filename(), source.sourceUri()),
                    text,
                    ContentFamily.STRUCTURED_TABLE,
                    parsedMetadata
            );
        } catch (IOException | TikaException exception) {
            throw new IllegalStateException("表格深度解析失败: " + source.sourceUri(), exception);
        }
    }

    private String parseSpreadsheet(DocumentSource source) throws IOException {
        StringBuilder builder = new StringBuilder();
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
                    builder.append(formatRowGroup(headers, values)).append('\n');
                }
                builder.append('\n');
            }
        }
        return builder.toString();
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

    private static int countRowGroups(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return (int) text.lines().filter(line -> line.contains("=")).count();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "untitled";
    }
}
