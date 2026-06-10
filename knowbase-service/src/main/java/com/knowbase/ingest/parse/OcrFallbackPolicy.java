package com.knowbase.ingest.parse;

/**
 * 判断是否在 Tika 抽取后启用 OCR 回退。
 */
public final class OcrFallbackPolicy {

    private OcrFallbackPolicy() {
    }

    public static boolean shouldFallback(String extractedText, String mimeType, String fileName, int minCharsToSkip) {
        if (!isOcrEligibleMime(mimeType, fileName)) {
            return false;
        }
        int length = extractedText == null ? 0 : extractedText.trim().length();
        return length < Math.max(1, minCharsToSkip);
    }

    public static boolean isOcrEligibleMime(String mimeType, String fileName) {
        if (mimeType != null) {
            String mime = mimeType.toLowerCase();
            if ("application/pdf".equals(mime) || mime.startsWith("image/")) {
                return true;
            }
        }
        if (fileName == null) {
            return false;
        }
        String lower = fileName.toLowerCase();
        return lower.endsWith(".pdf")
                || lower.endsWith(".png")
                || lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".tif")
                || lower.endsWith(".tiff")
                || lower.endsWith(".bmp")
                || lower.endsWith(".gif");
    }
}
