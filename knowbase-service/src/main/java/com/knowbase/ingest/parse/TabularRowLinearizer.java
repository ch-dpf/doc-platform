package com.knowbase.ingest.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 将 Tab 分隔或 Markdown 表格块转换为「列名: 值」行，便于向量检索理解列语义。
 */
public final class TabularRowLinearizer {

    private static final Pattern DATA_ROW = Pattern.compile("^\\d+\\t");
    private static final Pattern MARKDOWN_ROW = Pattern.compile("^\\|(.+)\\|$");

    private TabularRowLinearizer() {}

    public static boolean looksTabular(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        boolean hasTabRow = false;
        boolean hasDataRow = false;
        for (String raw : text.replace("\r\n", "\n").replace('\r', '\n').split("\n")) {
            String line = raw.strip();
            if (line.isEmpty()) {
                continue;
            }
            if (line.contains("\t")) {
                hasTabRow = true;
            }
            if (DATA_ROW.matcher(line).find()) {
                hasDataRow = true;
            }
            if (MARKDOWN_ROW.matcher(line).find() && line.contains("|")) {
                hasTabRow = true;
            }
        }
        return hasTabRow && hasDataRow;
    }

    public static String linearize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n').strip();
        if (isMarkdownTableBlock(normalized)) {
            return linearizeMarkdownTable(normalized);
        }

        List<String> parts = new ArrayList<>();
        String headerLine = null;
        List<String> dataRows = new ArrayList<>();

        for (String raw : normalized.split("\n")) {
            String line = raw.strip();
            if (line.isEmpty()) {
                continue;
            }
            if (isTableHeaderLine(line)) {
                flushTsvTable(parts, headerLine, dataRows);
                headerLine = line;
                dataRows.clear();
            } else if (headerLine != null && isTableDataLine(line)) {
                dataRows.add(line);
            } else {
                flushTsvTable(parts, headerLine, dataRows);
                headerLine = null;
                dataRows.clear();
                parts.add(line);
            }
        }
        flushTsvTable(parts, headerLine, dataRows);
        return String.join("\n\n", parts);
    }

    private static void flushTsvTable(List<String> parts, String headerLine, List<String> dataRows) {
        if (headerLine == null) {
            return;
        }
        if (dataRows.isEmpty()) {
            parts.add(headerLine);
            return;
        }
        String[] headers = splitTsvCells(headerLine);
        for (String dataRow : dataRows) {
            String linearized = linearizeTsvRow(headers, splitTsvCells(dataRow));
            if (!linearized.isEmpty()) {
                parts.add(linearized);
            }
        }
    }

    private static String linearizeTsvRow(String[] headers, String[] values) {
        if (headers.length == 0 || values.length == 0) {
            return String.join("\t", values);
        }
        StringBuilder row = new StringBuilder();
        int columns = Math.max(headers.length, values.length);
        for (int i = 0; i < columns; i++) {
            String header = i < headers.length ? headers[i].strip() : "";
            String value = i < values.length ? values[i].strip() : "";
            if (header.isEmpty() && value.isEmpty()) {
                continue;
            }
            if (!row.isEmpty()) {
                row.append(" | ");
            }
            if (header.isEmpty()) {
                row.append(value);
            } else {
                row.append(header).append(": ").append(value);
            }
        }
        return row.toString().strip();
    }

    private static String linearizeMarkdownTable(String block) {
        List<String> lines = new ArrayList<>();
        for (String raw : block.split("\n")) {
            String line = raw.strip();
            if (!line.isEmpty() && isMarkdownTableLine(line) && !isMarkdownSeparator(line)) {
                lines.add(line);
            }
        }
        if (lines.size() < 2) {
            return block;
        }
        String[] headers = parseMarkdownCells(lines.get(0));
        List<String> rows = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String linearized = linearizeTsvRow(headers, parseMarkdownCells(lines.get(i)));
            if (!linearized.isEmpty()) {
                rows.add(linearized);
            }
        }
        return rows.isEmpty() ? block : String.join("\n\n", rows);
    }

    private static boolean isMarkdownTableBlock(String block) {
        if (block == null || block.isBlank()) {
            return false;
        }
        boolean sawRow = false;
        for (String raw : block.split("\n")) {
            String line = raw.strip();
            if (line.isEmpty()) {
                continue;
            }
            if (!isMarkdownTableLine(line)) {
                return false;
            }
            if (!isMarkdownSeparator(line)) {
                sawRow = true;
            }
        }
        return sawRow;
    }

    private static boolean isMarkdownTableLine(String line) {
        return line != null && MARKDOWN_ROW.matcher(line.strip()).matches();
    }

    private static boolean isMarkdownSeparator(String line) {
        if (line == null) {
            return false;
        }
        String trimmed = line.strip();
        return trimmed.matches("^\\|[-: |]+\\|$");
    }

    private static String[] parseMarkdownCells(String line) {
        String inner = line.strip();
        if (inner.startsWith("|")) {
            inner = inner.substring(1);
        }
        if (inner.endsWith("|")) {
            inner = inner.substring(0, inner.length() - 1);
        }
        String[] parts = inner.split("\\|", -1);
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].strip();
        }
        return parts;
    }

    private static String[] splitTsvCells(String line) {
        return line.split("\t", -1);
    }

    public static boolean isTableHeaderLine(String line) {
        if (line == null || !line.contains("\t") || DATA_ROW.matcher(line).find()) {
            return false;
        }
        return countNonEmptyCells(splitTsvCells(line)) >= 2;
    }

    public static boolean isTableDataLine(String line) {
        if (line == null || !line.contains("\t")) {
            return false;
        }
        return DATA_ROW.matcher(line).find() || countNonEmptyCells(splitTsvCells(line)) >= 2;
    }

    private static int countNonEmptyCells(String[] cells) {
        int count = 0;
        for (String cell : cells) {
            if (cell != null && !cell.strip().isEmpty()) {
                count++;
            }
        }
        return count;
    }
}
