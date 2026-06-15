package com.knowbase.ingest.parse;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 将 Tika 等解析器输出的 Excel 日期序列号（如 45919）转为可读日期。
 * <p>
 * 按表头识别日期列（时间/日期/date/time 等），仅转换列语义为日期的单元格，避免误伤序号、编号等数字列。
 */
public final class ExcelSerialDateNormalizer {

    private static final LocalDate EXCEL_EPOCH = LocalDate.of(1899, 12, 30);
    private static final int MIN_SERIAL = 25_000;
    private static final int MAX_SERIAL = 60_000;
    private static final Pattern SERIAL_CELL = Pattern.compile("^(\\d{4,5})(?:\\.0)?$");
    private static final Pattern MARKDOWN_ROW = Pattern.compile("^\\|(.+)\\|$");
    private static final Pattern DATE_HEADER = Pattern.compile(
            ".*(时间|日期|\\bdate\\b|\\btime\\b|截止|到期|deadline|\\bdue\\b).*",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern FORMATTED_DATE = Pattern.compile(
            "^\\d{4}(?:[.\\-/年]\\d{1,2}(?:[.\\-/月]\\d{1,2}(?:日)?)?|\\d{2}[.\\-/]\\d{2})$");

    private static final Pattern NUMBERED_DATA_ROW = Pattern.compile("^\\d+\\t");

    private ExcelSerialDateNormalizer() {}

    public static String normalize(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n", -1);
        if (lines.length == 0) {
            return text;
        }

        boolean[] dateColumns = null;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                out.append('\n');
            }
            ProcessedLine processed = processLine(lines[i], dateColumns);
            dateColumns = processed.dateColumns();
            out.append(processed.line());
        }
        return out.toString();
    }

    private record ProcessedLine(String line, boolean[] dateColumns) {}

    private static ProcessedLine processLine(String rawLine, boolean[] dateColumns) {
        if (rawLine == null) {
            return new ProcessedLine("", dateColumns);
        }
        String line = rawLine;
        if (isMarkdownSeparator(line)) {
            return new ProcessedLine(line, dateColumns);
        }

        if (isMarkdownRow(line)) {
            List<String> cells = parseMarkdownCells(line);
            if (cells.isEmpty()) {
                return new ProcessedLine(line, dateColumns);
            }
            boolean header = dateColumns == null && looksLikeHeaderRow(cells);
            boolean[] effectiveColumns = header ? buildDateColumns(cells) : dateColumns;
            if (effectiveColumns == null) {
                return new ProcessedLine(line, dateColumns);
            }
            List<String> converted = convertCells(cells, effectiveColumns, header);
            return new ProcessedLine(formatMarkdownRow(converted), header ? effectiveColumns : dateColumns);
        }

        if (!line.contains("\t")) {
            return new ProcessedLine(line, dateColumns);
        }

        String[] cells = line.split("\t", -1);
        boolean header = isTableHeaderLine(line);
        boolean[] effectiveColumns = header ? buildDateColumns(List.of(cells)) : dateColumns;
        if (effectiveColumns == null) {
            return new ProcessedLine(line, dateColumns);
        }
        List<String> converted = convertCells(List.of(cells), effectiveColumns, header);
        return new ProcessedLine(joinTsv(converted), header ? effectiveColumns : dateColumns);
    }

    private static boolean[] buildDateColumns(List<String> headers) {
        boolean[] dateColumns = new boolean[headers.size()];
        boolean any = false;
        for (int i = 0; i < headers.size(); i++) {
            dateColumns[i] = isDateLikeHeader(headers.get(i));
            any |= dateColumns[i];
        }
        return any ? dateColumns : null;
    }

    private static List<String> convertCells(List<String> cells, boolean[] dateColumns, boolean headerRow) {
        List<String> converted = new ArrayList<>(cells.size());
        for (int i = 0; i < cells.size(); i++) {
            String cell = cells.get(i);
            if (headerRow || i >= dateColumns.length || !dateColumns[i]) {
                converted.add(cell);
                continue;
            }
            converted.add(formatSerialCell(cell));
        }
        return converted;
    }

    static String formatSerialCell(String cell) {
        if (cell == null || cell.isBlank() || FORMATTED_DATE.matcher(cell.strip()).find()) {
            return cell;
        }
        String trimmed = cell.strip();
        var matcher = SERIAL_CELL.matcher(trimmed);
        if (!matcher.matches()) {
            return cell;
        }
        int serial = Integer.parseInt(matcher.group(1));
        if (serial < MIN_SERIAL || serial > MAX_SERIAL) {
            return cell;
        }
        LocalDate date = EXCEL_EPOCH.plusDays(serial);
        return date.getYear() + "." + date.getMonthValue() + "." + date.getDayOfMonth();
    }

    static boolean isDateLikeHeader(String header) {
        if (header == null || header.isBlank()) {
            return false;
        }
        return DATE_HEADER.matcher(header.strip()).matches();
    }

    private static boolean isTableHeaderLine(String line) {
        if (isWeeklyMetadataLine(line) || looksLikeDataRow(line)) {
            return false;
        }
        return TabularRowLinearizer.isTableHeaderLine(line);
    }

    private static boolean looksLikeDataRow(String line) {
        if (line == null || !line.contains("\t")) {
            return false;
        }
        if (NUMBERED_DATA_ROW.matcher(line).find()) {
            return true;
        }
        String firstCell = line.split("\t", -1)[0].strip();
        if (firstCell.matches("^\\d+$")) {
            return true;
        }
        return firstCell.matches("^[A-Za-z]{1,}[-_]\\d+.*")
                || firstCell.matches("^[A-Z]{2,}\\d+$")
                || firstCell.matches("^[A-Za-z0-9]{6,}$");
    }

    private static boolean isWeeklyMetadataLine(String line) {
        return line != null
                && line.contains("部门\t")
                && (line.contains("姓名\t") || line.contains("部门负责人\t"));
    }

    private static boolean looksLikeHeaderRow(List<String> cells) {
        if (cells.stream().filter(cell -> cell != null && !cell.isBlank()).count() < 2) {
            return false;
        }
        String joined = String.join("\t", cells);
        return TabularRowLinearizer.isTableHeaderLine(joined);
    }

    private static boolean isMarkdownRow(String line) {
        return line != null && MARKDOWN_ROW.matcher(line.strip()).matches();
    }

    private static boolean isMarkdownSeparator(String line) {
        return line != null && line.strip().matches("^\\|[-: |]+\\|$");
    }

    private static List<String> parseMarkdownCells(String line) {
        String inner = line.strip();
        if (inner.startsWith("|")) {
            inner = inner.substring(1);
        }
        if (inner.endsWith("|")) {
            inner = inner.substring(0, inner.length() - 1);
        }
        String[] parts = inner.split("\\|", -1);
        List<String> cells = new ArrayList<>(parts.length);
        for (String part : parts) {
            cells.add(part.strip());
        }
        return cells;
    }

    private static String formatMarkdownRow(List<String> cells) {
        StringBuilder row = new StringBuilder("|");
        for (String cell : cells) {
            row.append(' ').append(cell).append(" |");
        }
        return row.toString();
    }

    private static String joinTsv(List<String> cells) {
        StringBuilder row = new StringBuilder();
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                row.append('\t');
            }
            row.append(cells.get(i));
        }
        return row.toString();
    }
}
