package com.knowbase.vector.chunk;

import java.util.regex.Pattern;

/**
 * 合并 Tika 解析 Excel 时因单元格内换行产生的「续行」。
 * <p>
 * 例如周报第 2 行说明列「第一点…；」与「第二点…」被拆成两段空行分隔的文本时，
 * 在分块前合并回同一 Tab 数据行，避免段落分块拦腰切断。
 */
public final class TabularContinuationNormalizer {

    private static final Pattern DATA_ROW = Pattern.compile("^\\d+\\t");
    private static final Pattern SECTION_ROW = Pattern.compile(
            "^(序号\\t|部门\\t|周报|星图|\\d{4}年|Sheet\\d+$)");

    private TabularContinuationNormalizer() {}

    public static String joinContinuations(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String[] lines = text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        StringBuilder out = new StringBuilder();
        StringBuilder current = null;

        for (String raw : lines) {
            String line = raw.strip();
            if (line.isEmpty()) {
                continue;
            }
            if (isDataRowStart(line) || isSectionStart(line)) {
                flush(out, current);
                current = new StringBuilder(line);
            } else if (isContinuation(line, current)) {
                current.append(' ').append(line);
            } else {
                flush(out, current);
                current = new StringBuilder(line);
            }
        }
        flush(out, current);
        return out.toString().strip();
    }

    static boolean isDataRowStart(String line) {
        return line != null && DATA_ROW.matcher(line).find();
    }

    static boolean isSectionStart(String line) {
        return line != null && SECTION_ROW.matcher(line).find();
    }

    static boolean isContinuation(String line, CharSequence currentRow) {
        if (line == null || line.isBlank() || currentRow == null || currentRow.isEmpty()) {
            return false;
        }
        if (isDataRowStart(line) || isSectionStart(line)) {
            return false;
        }
        return DATA_ROW.matcher(currentRow).find();
    }

    private static void flush(StringBuilder out, StringBuilder current) {
        if (current == null || current.isEmpty()) {
            return;
        }
        if (!out.isEmpty()) {
            out.append("\n\n");
        }
        out.append(current);
        current.setLength(0);
    }
}
