package com.knowbase.pipeline.content;

/**
 * MIME / 文件名 → {@link ContentFamily} 映射（FILE-TYPE-PROCESSING 附录 A 的族群化）。
 */
public final class ContentFamilyResolver {

    private ContentFamilyResolver() {
    }

    public static ContentFamily resolve(String mimeType) {
        return resolve(mimeType, null);
    }

    public static ContentFamily resolve(String mimeType, String fileName) {
        if (mimeType != null) {
            String m = mimeType.toLowerCase();
            if (isTabular(m)) {
                return ContentFamily.TABULAR;
            }
            if (isDocument(m)) {
                return ContentFamily.DOCUMENT;
            }
            if (isPlain(m)) {
                return ContentFamily.PLAIN;
            }
            if (m.startsWith("image/")) {
                return ContentFamily.IMAGE;
            }
        }
        if (fileName != null) {
            String lower = fileName.toLowerCase();
            if (lower.endsWith(".xls")
                    || lower.endsWith(".xlsx")
                    || lower.endsWith(".csv")
                    || lower.endsWith(".tsv")) {
                return ContentFamily.TABULAR;
            }
            if (lower.endsWith(".pdf") || lower.endsWith(".doc") || lower.endsWith(".docx")) {
                return ContentFamily.DOCUMENT;
            }
            if (lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".markdown")) {
                return ContentFamily.PLAIN;
            }
            if (lower.endsWith(".png")
                    || lower.endsWith(".jpg")
                    || lower.endsWith(".jpeg")
                    || lower.endsWith(".tif")
                    || lower.endsWith(".tiff")
                    || lower.endsWith(".bmp")
                    || lower.endsWith(".gif")) {
                return ContentFamily.IMAGE;
            }
        }
        return ContentFamily.UNKNOWN;
    }

    private static boolean isTabular(String mime) {
        return mime.contains("spreadsheet")
                || mime.contains("excel")
                || mime.endsWith(".sheet")
                || "text/csv".equals(mime)
                || "text/tab-separated-values".equals(mime);
    }

    private static boolean isDocument(String mime) {
        return mime.contains("pdf")
                || mime.contains("word")
                || mime.contains("msword")
                || mime.contains("document");
    }

    private static boolean isPlain(String mime) {
        return mime.contains("text/plain") || mime.contains("markdown") || "text/x-markdown".equals(mime);
    }
}
