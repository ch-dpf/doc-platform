package com.knowbase.ingestion.pdf;

import java.util.Locale;
import java.util.regex.Pattern;

public final class PdfLayoutRoleClassifier {

    private static final Pattern PAGE_FOOTER = Pattern.compile("(?im)^\\s*(?:page\\s+\\d+|第\\s*\\d+\\s*页(?:\\s*/\\s*\\d+\\s*页)?)\\s*$");
    private static final Pattern STANDALONE_PAGE_NUMBER = Pattern.compile("(?m)^\\s*\\d{1,4}\\s*$");

    private PdfLayoutRoleClassifier() {
    }

    public static String classify(String content, float y, float pageHeight, float avgFont, float bodyFontSize) {
        if (content == null || content.isBlank()) {
            return "body";
        }
        if (isPageFooter(content, y, pageHeight)) {
            return "footer";
        }
        if (isPageHeader(content, y, pageHeight)) {
            return "header";
        }
        if (avgFont >= bodyFontSize * 1.18f && content.length() <= 120) {
            return avgFont >= bodyFontSize * 1.35f ? "title" : "heading";
        }
        return "body";
    }

    public static boolean isPageFooter(String content, float y, float pageHeight) {
        if (pageHeight > 0 && y < pageHeight * 0.12f) {
            return PAGE_FOOTER.matcher(content).matches() || STANDALONE_PAGE_NUMBER.matcher(content).matches();
        }
        return PAGE_FOOTER.matcher(content).matches();
    }

    public static boolean isPageHeader(String content, float y, float pageHeight) {
        return pageHeight > 0 && y > pageHeight * 0.88f && content.length() <= 80;
    }

    public static boolean isIndexableRole(String layoutRole) {
        if (layoutRole == null) {
            return true;
        }
        String normalized = layoutRole.toLowerCase(Locale.ROOT);
        return !"footer".equals(normalized) && !"header".equals(normalized);
    }
}
