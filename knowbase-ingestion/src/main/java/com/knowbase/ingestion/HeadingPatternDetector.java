package com.knowbase.ingestion;

import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 识别常见文档大纲标题（与 MaxKB/Dify 等主流 RAG 对 DOCX/手册的处理一致），
 * 不依赖 Word 内置 Heading 样式。
 */
public final class HeadingPatternDetector {

    private static final int MAX_HEADING_CHARS = 100;

    private static final Pattern MARKDOWN_HEADING = Pattern.compile("^(#{1,6})\\s+(.+)$");
    private static final Pattern CHINESE_CHAPTER = Pattern.compile("^[一二三四五六七八九十百]+[、.]\\s*\\S.*");
    private static final Pattern DIGIT_CHAPTER = Pattern.compile("^\\d+、\\s*\\S.*");
    private static final Pattern CHAPTER = Pattern.compile("^第\\s*[\\d一二三四五六七八九十百]+\\s*[章节篇部].*");
    private static final Pattern NUMBERED_SECTION = Pattern.compile("^(\\d+(?:\\.\\d+)+)\\s+\\S.*");
    private static final Pattern SINGLE_NUMBER_SECTION = Pattern.compile("^(\\d+)\\s+\\S.{2,80}$");
    private static final Pattern LIST_ITEM = Pattern.compile("^[☐☑□▪•●◦\\-*+]\\s+.*");

    private HeadingPatternDetector() {
    }

    public static boolean hasOutlineStructure(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        int hits = 0;
        for (String line : text.split("\n", -1)) {
            if (detectLevel(line).isPresent()) {
                hits++;
                if (hits >= 2) {
                    return true;
                }
            }
        }
        return false;
    }

    public static OptionalInt detectLevel(String text) {
        if (text == null || text.isBlank()) {
            return OptionalInt.empty();
        }
        String line = text.trim();
        if (line.length() > MAX_HEADING_CHARS) {
            return OptionalInt.empty();
        }
        if (LIST_ITEM.matcher(line).matches()) {
            return OptionalInt.empty();
        }
        if (line.endsWith("；") || line.endsWith(";")) {
            if (line.length() > 30) {
                return OptionalInt.empty();
            }
        }

        Matcher markdown = MARKDOWN_HEADING.matcher(line);
        if (markdown.matches()) {
            return OptionalInt.of(markdown.group(1).length());
        }
        if (CHAPTER.matcher(line).matches()) {
            return OptionalInt.of(1);
        }
        if (CHINESE_CHAPTER.matcher(line).matches()) {
            return OptionalInt.of(1);
        }
        Matcher numbered = NUMBERED_SECTION.matcher(line);
        if (numbered.matches()) {
            return OptionalInt.of(levelFromNumberPath(numbered.group(1)));
        }
        if (DIGIT_CHAPTER.matcher(line).matches()) {
            return OptionalInt.of(1);
        }

        Matcher singleNumber = SINGLE_NUMBER_SECTION.matcher(line);
        if (singleNumber.matches()) {
            return OptionalInt.of(2);
        }
        return OptionalInt.empty();
    }

    public static boolean isListParagraphStyle(String styleId) {
        if (styleId == null || styleId.isBlank()) {
            return false;
        }
        String normalized = styleId.toLowerCase();
        return normalized.contains("listbullet")
                || normalized.contains("listnumber")
                || normalized.contains("列表");
    }

    private static int levelFromNumberPath(String numberPath) {
        int depth = numberPath.split("\\.").length;
        return Math.min(6, Math.max(1, depth));
    }
}
