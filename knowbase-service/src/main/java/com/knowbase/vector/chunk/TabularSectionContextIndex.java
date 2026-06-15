package com.knowbase.vector.chunk;

import com.knowbase.vector.rag.RagEmployeeNameExtractor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从表格类全文解析 section 上下文，并按数据行建立前缀索引。
 */
public final class TabularSectionContextIndex {

    private static final Pattern LEADING_TABS = Pattern.compile("^\t+");
    private static final Pattern SUBMITTER_FIELD =
            Pattern.compile("(?:^|[\\n\\r\\t])(?:姓名|部门负责人)(?:[：:]|[\\t])\\s*([\\u4e00-\\u9fff]{2,4})");
    private static final Pattern PERIOD_CN = Pattern.compile(
            "(\\d{4}\\s*年\\s*\\d{1,2}\\s*月\\s*\\d{1,2}\\s*日\\s*[-—~]+\\s*\\d{1,2}\\s*月\\s*\\d{1,2}\\s*日)");
    private static final Pattern PERIOD_SHORT = Pattern.compile(
            "(\\d{1,2}\\.\\d{1,2}\\s*[-—~]\\s*\\d{1,2}\\.\\d{1,2})");
    private static final Pattern EXISTING_PREFIX = Pattern.compile("^【[^】]+】");

    private final Map<String, String> rowPrefixByLine;

    private TabularSectionContextIndex(Map<String, String> rowPrefixByLine) {
        this.rowPrefixByLine = rowPrefixByLine;
    }

    public static TabularSectionContextIndex parse(String text) {
        return parse(text, null);
    }

    public static TabularSectionContextIndex parse(String text, String fileName) {
        List<TabularTableScanner.RowBinding> bindings = TabularTableScanner.collectDataRows(text, fileName);
        Map<String, String> index = new LinkedHashMap<>();
        for (TabularTableScanner.RowBinding binding : bindings) {
            String prefix = binding.context().formatPrefix();
            if (!prefix.isBlank()) {
                index.put(binding.line(), prefix);
            }
        }
        return new TabularSectionContextIndex(Map.copyOf(index));
    }

    public boolean isEmpty() {
        return rowPrefixByLine.isEmpty();
    }

    public String injectPrefix(String chunk) {
        if (chunk == null || chunk.isBlank() || rowPrefixByLine.isEmpty()) {
            return chunk;
        }
        if (EXISTING_PREFIX.matcher(chunk.strip()).find()) {
            return chunk;
        }
        String prefix = resolvePrefix(chunk);
        if (prefix == null || prefix.isBlank()) {
            return chunk;
        }
        return prefix + "\n\n" + chunk.strip();
    }

    private String resolvePrefix(String chunk) {
        for (String raw : chunk.replace("\r\n", "\n").replace('\r', '\n').split("\n")) {
            String line = normalizeLine(raw);
            if (line.isEmpty()) {
                continue;
            }
            String exact = rowPrefixByLine.get(line);
            if (exact != null) {
                return exact;
            }
            for (Map.Entry<String, String> entry : rowPrefixByLine.entrySet()) {
                if (line.startsWith(rowKeyPrefix(entry.getKey()))) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    static String normalizeLine(String raw) {
        if (raw == null) {
            return "";
        }
        String line = raw.replace('\r', '\n').strip();
        return LEADING_TABS.matcher(line).replaceAll("").strip();
    }

    static String detectSectionLabel(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }
        if (line.contains("周工作计划") || line.contains("工作计划")) {
            return "周工作计划";
        }
        if (line.contains("工作周报")) {
            return "工作周报";
        }
        return null;
    }

    static String extractSubmitter(String line) {
        Matcher matcher = SUBMITTER_FIELD.matcher(line);
        if (matcher.find() && RagEmployeeNameExtractor.looksLikePersonName(matcher.group(1))) {
            return matcher.group(1).strip();
        }
        return null;
    }

    static String extractPeriod(String line) {
        Matcher cn = PERIOD_CN.matcher(line);
        if (cn.find()) {
            return cn.group(1).replaceAll("\\s+", "");
        }
        Matcher shortRange = PERIOD_SHORT.matcher(line);
        if (shortRange.find()) {
            return shortRange.group(1).replaceAll("\\s+", "");
        }
        return null;
    }

    static String compactColumns(String headerLine) {
        String[] cells = headerLine.split("\t", -1);
        StringBuilder out = new StringBuilder();
        for (String cell : cells) {
            String value = cell.strip();
            if (value.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append('|');
            }
            out.append(value);
        }
        return out.toString();
    }

    private static String rowKeyPrefix(String rowLine) {
        if (rowLine == null) {
            return "";
        }
        int tab = rowLine.indexOf('\t');
        if (tab < 0) {
            return rowLine.length() > 48 ? rowLine.substring(0, 48) : rowLine;
        }
        int secondTab = rowLine.indexOf('\t', tab + 1);
        if (secondTab < 0) {
            return rowLine;
        }
        return rowLine.substring(0, secondTab);
    }
}
