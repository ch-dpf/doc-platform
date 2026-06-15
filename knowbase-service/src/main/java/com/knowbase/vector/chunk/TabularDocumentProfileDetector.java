package com.knowbase.vector.chunk;

/**
 * 识别表格文档是周报模板还是通用表。
 */
public final class TabularDocumentProfileDetector {

    private TabularDocumentProfileDetector() {}

    public static TabularDocumentProfile detect(String text, String fileName) {
        if (looksLikeWeeklyReportFileName(fileName)) {
            return TabularDocumentProfile.WEEKLY_REPORT;
        }
        if (looksLikeWeeklyReportText(text)) {
            return TabularDocumentProfile.WEEKLY_REPORT;
        }
        return TabularDocumentProfile.GENERIC;
    }

    private static boolean looksLikeWeeklyReportFileName(String fileName) {
        return fileName != null && !fileName.isBlank() && fileName.contains("周报");
    }

    private static boolean looksLikeWeeklyReportText(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        boolean weeklySection = text.contains("工作周报") || text.contains("周工作计划");
        boolean weeklyHeader = text.contains("序号") && text.contains("工作内容");
        return weeklySection && weeklyHeader;
    }
}
