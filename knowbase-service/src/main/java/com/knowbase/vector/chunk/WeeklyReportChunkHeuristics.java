package com.knowbase.vector.chunk;

import java.util.regex.Pattern;

/** 识别周报 Excel 分块中的「仅表头、无数据行」片段。 */
public final class WeeklyReportChunkHeuristics {

    private static final Pattern WORK_ROW = Pattern.compile("(?m)^\\d+\\t[^\\t\\n]+\\t([^\\t\\n]+)");

    private WeeklyReportChunkHeuristics() {}

    public static boolean isHeaderOnlyChunk(String content) {
        if (content == null || content.isBlank()) {
            return true;
        }
        boolean hasHeader = content.contains("序号") && content.contains("工作内容");
        if (!hasHeader) {
            return false;
        }
        return !WORK_ROW.matcher(content).find();
    }
}
