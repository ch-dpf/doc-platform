package com.knowbase.ingest.parse;

import java.util.Locale;

/**
 * Excel MIME / 扩展名识别，供结构化解析路由使用。
 */
public final class ExcelMimeSupport {

    private ExcelMimeSupport() {}

    public static boolean isExcel(String mimeType, String fileName) {
        if (mimeType != null && !mimeType.isBlank()) {
            String m = mimeType.trim().toLowerCase(Locale.ROOT);
            if (m.contains("spreadsheet") || m.contains("excel") || m.endsWith(".sheet")) {
                return true;
            }
        }
        if (fileName == null || fileName.isBlank()) {
            return false;
        }
        String lower = fileName.trim().toLowerCase(Locale.ROOT);
        return lower.endsWith(".xls") || lower.endsWith(".xlsx");
    }
}
