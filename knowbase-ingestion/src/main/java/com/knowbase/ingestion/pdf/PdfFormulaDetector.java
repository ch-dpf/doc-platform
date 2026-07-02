package com.knowbase.ingestion.pdf;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Heuristic detection of inline/display mathematical notation in PDF text lines.
 */
public final class PdfFormulaDetector {

    private static final Pattern INLINE_DOLLAR = Pattern.compile("\\$([^$\\n]{1,200})\\$");
    private static final Pattern DISPLAY_BRACKET = Pattern.compile("\\\\\\[([^\\]]{1,500})\\\\\\]");
    private static final Pattern INLINE_PAREN = Pattern.compile("\\\\\\(([^)]{1,200})\\\\\\)");
    private static final Pattern LATEX_COMMAND = Pattern.compile(
            "\\\\(frac|sum|int|sqrt|alpha|beta|gamma|Delta|pi|theta|lambda|infty|partial|nabla|cdot|times|leq|geq|neq|approx)\\b"
    );

    private PdfFormulaDetector() {
    }

    public record FormulaMatch(String latex, String format, boolean display) {
    }

    public static boolean isFormulaLike(String text) {
        return detect(text) != null;
    }

    public static FormulaMatch detect(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String trimmed = text.trim();
        Matcher display = DISPLAY_BRACKET.matcher(trimmed);
        if (display.find()) {
            return new FormulaMatch(display.group(1).trim(), "latex", true);
        }
        Matcher inlineParen = INLINE_PAREN.matcher(trimmed);
        if (inlineParen.find()) {
            return new FormulaMatch(inlineParen.group(1).trim(), "latex", false);
        }
        Matcher inlineDollar = INLINE_DOLLAR.matcher(trimmed);
        if (inlineDollar.find()) {
            return new FormulaMatch(inlineDollar.group(1).trim(), "latex", false);
        }
        if (LATEX_COMMAND.matcher(trimmed).find() && trimmed.length() <= 240) {
            return new FormulaMatch(trimmed, "latex", trimmed.length() > 80);
        }
        return null;
    }
}
