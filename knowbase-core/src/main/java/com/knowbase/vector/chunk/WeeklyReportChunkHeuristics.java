package com.knowbase.vector.chunk;

import java.util.regex.Pattern;

/** 识别周报 Excel 分块中的「仅表头、无数据行」片段。 */
public final class WeeklyReportChunkHeuristics {

    private static final Pattern TSV_WORK_ROW = Pattern.compile("(?m)^\\d+\\t[^\\t\\n]+\\t([^\\t\\n]+)");
    private static final Pattern KEY_VALUE_WORK_ROW = Pattern.compile("(?m)^序号:\\s*\\d+\\b.*工作内容:");

    private WeeklyReportChunkHeuristics() {}

    public static boolean isHeaderOnlyChunk(String content) {
        if (content == null || content.isBlank()) {
            return true;
        }
        boolean hasHeader = content.contains("序号") && content.contains("工作内容");
        if (!hasHeader) {
            return false;
        }
        return !hasWorkDataRow(content);
    }

    static boolean hasWorkDataRow(String content) {
        return TSV_WORK_ROW.matcher(content).find() || KEY_VALUE_WORK_ROW.matcher(content).find();
    }
}
