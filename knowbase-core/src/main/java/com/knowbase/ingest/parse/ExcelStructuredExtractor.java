package com.knowbase.ingest.parse;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Excel structured 模式：优先 Tika HTML 表格路径，无表格时回退 POI 生成 Markdown。
 */
public final class ExcelStructuredExtractor {

    private static final Logger log = LoggerFactory.getLogger(ExcelStructuredExtractor.class);

    private ExcelStructuredExtractor() {}

    public static String extract(
            byte[] bytes,
            String fileName,
            DocumentParseOptions options,
            Function<ExtractRequest, String> htmlExtractor) {
        DocumentParseOptions effective = options != null ? options : DocumentParseOptions.disabled();
        try {
            String html = htmlExtractor.apply(new ExtractRequest(bytes, fileName, effective));
            String processed = HtmlParsingContentProcessor.apply(html, effective);
            if (containsMarkdownTable(processed)) {
                return processed;
            }
        } catch (Exception e) {
            log.debug("Tika HTML table path failed for {}: {}", fileName, e.getMessage());
        }
        return extractWithPoi(bytes, fileName);
    }

    private static boolean containsMarkdownTable(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return text.lines().anyMatch(line -> line.strip().startsWith("|") && line.strip().endsWith("|"));
    }

    private static String extractWithPoi(byte[] bytes, String fileName) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes);
                Workbook workbook = WorkbookFactory.create(in)) {
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            StringBuilder output = new StringBuilder();
            int sheetCount = workbook.getNumberOfSheets();
            for (int s = 0; s < sheetCount; s++) {
                Sheet sheet = workbook.getSheetAt(s);
                if (sheet == null) {
                    continue;
                }
                String markdown = sheetToMarkdown(sheet, formatter);
                if (markdown.isBlank()) {
                    continue;
                }
                if (!output.isEmpty()) {
                    output.append("\n\n");
                }
                if (sheetCount > 1) {
                    String sheetName = sheet.getSheetName();
                    if (sheetName != null && !sheetName.isBlank()) {
                        output.append(sheetName.trim()).append('\n').append('\n');
                    }
                }
                output.append(markdown);
            }
            return output.toString().trim();
        } catch (Exception e) {
            throw new com.knowbase.ingest.service.ParseException(
                    "Failed to parse Excel with structured extraction: " + fileName, e);
        }
    }

    private static String sheetToMarkdown(Sheet sheet, DataFormatter formatter) {
        List<List<String>> rows = new ArrayList<>();
        int firstRow = sheet.getFirstRowNum();
        int lastRow = sheet.getLastRowNum();
        if (lastRow < firstRow) {
            return "";
        }

        int maxColumns = 0;
        for (int r = firstRow; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            List<String> cells = readRow(row, formatter);
            if (cells.stream().allMatch(String::isBlank)) {
                continue;
            }
            maxColumns = Math.max(maxColumns, cells.size());
            rows.add(cells);
        }
        if (rows.isEmpty() || maxColumns == 0) {
            return "";
        }

        StringBuilder markdown = new StringBuilder();
        appendMarkdownRow(markdown, padRow(rows.get(0), maxColumns));
        appendMarkdownSeparator(markdown, maxColumns);
        for (int i = 1; i < rows.size(); i++) {
            appendMarkdownRow(markdown, padRow(rows.get(i), maxColumns));
        }
        return markdown.toString().trim();
    }

    private static List<String> readRow(Row row, DataFormatter formatter) {
        List<String> cells = new ArrayList<>();
        int firstCell = row.getFirstCellNum();
        int lastCell = row.getLastCellNum();
        if (lastCell < 0) {
            return cells;
        }
        if (firstCell < 0) {
            firstCell = 0;
        }
        for (int c = firstCell; c < lastCell; c++) {
            Cell cell = row.getCell(c);
            String value = cell == null ? "" : formatter.formatCellValue(cell);
            cells.add(normalizeCell(value));
        }
        return cells;
    }

    private static List<String> padRow(List<String> row, int columnCount) {
        List<String> padded = new ArrayList<>(row);
        while (padded.size() < columnCount) {
            padded.add("");
        }
        if (padded.size() > columnCount) {
            return padded.subList(0, columnCount);
        }
        return padded;
    }

    private static void appendMarkdownRow(StringBuilder markdown, List<String> cells) {
        markdown.append('|');
        for (String cell : cells) {
            markdown.append(' ').append(escapeMarkdownCell(cell)).append(" |");
        }
        markdown.append('\n');
    }

    private static void appendMarkdownSeparator(StringBuilder markdown, int columnCount) {
        markdown.append('|');
        for (int i = 0; i < columnCount; i++) {
            markdown.append(" --- |");
        }
        markdown.append('\n');
    }

    private static String escapeMarkdownCell(String cell) {
        return cell.replace('|', '｜').replace('\n', ' ').replace('\r', ' ').trim();
    }

    private static String normalizeCell(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replace('\r', '\n').lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .reduce((a, b) -> a + " " + b)
                .orElse("");
    }

    public record ExtractRequest(byte[] bytes, String fileName, DocumentParseOptions options) {}
}
