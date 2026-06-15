package com.knowbase.ingest.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 内容清洗：页眉页脚、水印、脱敏、停用词。
 */
final class ContentCleaningEngine {

    private static final Pattern PHONE_PATTERN = Pattern.compile("(?<!\\d)(1[3-9]\\d{9})(?!\\d)");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("(?<!\\d)(\\d{17}[\\dXx])(?!\\d)");

    private static final List<Pattern> HEADER_FOOTER_LINE_PATTERNS = List.of(
            Pattern.compile("^\\d{1,4}$"),
            Pattern.compile("^第\\s*\\d+\\s*页(?:\\s*[/共]\\s*\\d+\\s*页)?$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
            Pattern.compile("^Page\\s+\\d+(\\s+of\\s+\\d+)?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^[-—_=]{3,}$"),
            Pattern.compile("^(?:Copyright|©|All Rights Reserved|版权所有|机密|内部资料).*$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));

    private static final Pattern WATERMARK_LINE_PATTERN = Pattern.compile(
            "^(?:【\\s*)?(?:样本|样张|草稿|试行|水印|内部资料|请勿外传|禁止外传|"
                    + "CONFIDENTIAL|DRAFT|WATERMARK|DO NOT COPY|NOT FOR DISTRIBUTION)(?:\\s*】)?$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Set<String> STOPWORDS = Set.of(
            "的", "了", "在", "是", "和", "与", "及", "或", "就", "都", "而", "着", "之", "等",
            "the", "this", "a", "an", "and", "or", "is", "are", "was", "were", "to", "of", "for", "in", "on", "at", "by");

    private ContentCleaningEngine() {}

    static String removeHeaderFooterLines(String text) {
        return dropMatchingLines(text, ContentCleaningEngine::isHeaderFooterLine);
    }

    static String removeWatermarkLines(String text) {
        return dropMatchingLines(text, ContentCleaningEngine::isWatermarkLine);
    }

    static String maskPhoneNumbers(String text) {
        Matcher matcher = PHONE_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String phone = matcher.group(1);
            String masked = phone.substring(0, 3) + "****" + phone.substring(7);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(masked));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    static String maskIdCardNumbers(String text) {
        Matcher matcher = ID_CARD_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String id = matcher.group(1);
            String masked = id.substring(0, 6) + "********" + id.substring(14);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(masked));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    static String filterStopwords(String text) {
        if (text == null || text.isBlank()) {
            return text == null ? "" : text.strip();
        }
        String[] lines = text.replace('\r', '\n').split("\n", -1);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                result.append('\n');
            }
            result.append(filterStopwordsInLine(lines[i]));
        }
        return result.toString();
    }

    private static String filterStopwordsInLine(String line) {
        if (line.isBlank()) {
            return line;
        }
        String[] tokens = line.trim().split("\\s+");
        if (tokens.length <= 1) {
            return line;
        }
        List<String> kept = new ArrayList<>(tokens.length);
        for (String token : tokens) {
            String normalized = token.toLowerCase(Locale.ROOT);
            if (!STOPWORDS.contains(token) && !STOPWORDS.contains(normalized)) {
                kept.add(token);
            }
        }
        if (kept.isEmpty()) {
            return "";
        }
        return String.join(" ", kept);
    }

    private static String dropMatchingLines(String text, LinePredicate predicate) {
        if (text == null || text.isBlank()) {
            return text == null ? "" : text.strip();
        }
        String[] lines = text.replace('\r', '\n').split("\n", -1);
        StringBuilder result = new StringBuilder();
        for (String line : lines) {
            if (predicate.test(line)) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append('\n');
            }
            result.append(line);
        }
        return normalizeConsecutiveBlankLines(result.toString());
    }

    private static boolean isHeaderFooterLine(String line) {
        String trimmed = line.strip();
        if (trimmed.isEmpty()) {
            return false;
        }
        for (Pattern pattern : HEADER_FOOTER_LINE_PATTERNS) {
            if (pattern.matcher(trimmed).matches()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isWatermarkLine(String line) {
        String trimmed = line.strip();
        if (trimmed.isEmpty()) {
            return false;
        }
        if (WATERMARK_LINE_PATTERN.matcher(trimmed).matches()) {
            return true;
        }
        if (trimmed.length() <= 32) {
            String lower = trimmed.toLowerCase(Locale.ROOT);
            return lower.contains("watermark")
                    || trimmed.contains("样本")
                    || trimmed.contains("草稿")
                    || trimmed.contains("内部资料");
        }
        return false;
    }

    private static String normalizeConsecutiveBlankLines(String text) {
        return text.replaceAll("\n{3,}", "\n\n").strip();
    }

    @FunctionalInterface
    private interface LinePredicate {
        boolean test(String line);
    }
}
