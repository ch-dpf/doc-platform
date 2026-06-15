package com.knowbase.ingest.support;

import java.util.List;
import java.util.Locale;

/**
 * 校验上传 MIME：白名单精确匹配，并对 Markdown 等浏览器/系统差异做扩展名兜底。
 */
public final class MimeTypeAllowlist {

    private MimeTypeAllowlist() {
    }

    public static boolean isAllowed(String mimeType, String fileName, List<String> allowedMimeTypes) {
        if (mimeType != null && allowedMimeTypes.contains(mimeType)) {
            return true;
        }
        if (isMarkdownFileName(fileName) && isMarkdownCompatibleMime(mimeType)) {
            return true;
        }
        return false;
    }

    public static boolean isMarkdownFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return false;
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".md") || lower.endsWith(".markdown");
    }

    public static boolean isMarkdownCompatibleMime(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return false;
        }
        String lower = mimeType.toLowerCase(Locale.ROOT);
        return lower.equals("text/plain")
                || lower.equals("application/octet-stream")
                || lower.contains("markdown");
    }
}
