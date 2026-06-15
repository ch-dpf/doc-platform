package com.knowbase.vector.chunk;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 按标题行切分文档为章节段落。
 */
public final class HeadingLevelChunker {

    /** 仅一级 Markdown 标题（单个 #，不含 ## 及以下） */
    private static final Pattern TOP_LEVEL_MARKDOWN = Pattern.compile("^#\\s+.+$");

    private static final Pattern HEADING_LINE = Pattern.compile(
            "^(?:"
                    + "#{1,6}\\s+.+"
                    + "|第[一二三四五六七八九十百零〇\\d]+[章节篇节部分][\\s:：].+"
                    + "|第[一二三四五六七八九十百零〇\\d]+[章节篇节部分]$"
                    + "|[一二三四五六七八九十]+[、．.]\\s*\\S+"
                    + "|\\d+(?:\\.\\d+)*[、．.]\\s*\\S+"
                    + ")$",
            Pattern.UNICODE_CASE);

    private HeadingLevelChunker() {}

    public static List<String> splitSections(String text) {
        return splitByHeading(text, HeadingLevelChunker::isHeading);
    }

    /** 按一级 Markdown 标题（#）切分，供语义分块等场景在主题边界预处理。 */
    public static List<String> splitTopLevelSections(String text) {
        return splitByHeading(text, HeadingLevelChunker::isTopLevelHeading);
    }

    private static List<String> splitByHeading(String text, java.util.function.Predicate<String> headingTest) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<String> sections = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : text.replace('\r', '\n').split("\n", -1)) {
            String trimmed = line.strip();
            if (headingTest.test(trimmed) && !current.isEmpty()) {
                appendSection(sections, current.toString());
                current.setLength(0);
            }
            if (!current.isEmpty()) {
                current.append('\n');
            }
            current.append(line);
        }
        appendSection(sections, current.toString());
        return sections;
    }

    private static boolean isHeading(String line) {
        if (line.isBlank() || line.length() > 120) {
            return false;
        }
        return HEADING_LINE.matcher(line).matches();
    }

    private static boolean isTopLevelHeading(String line) {
        if (line.isBlank() || line.length() > 120 || line.startsWith("##")) {
            return false;
        }
        return TOP_LEVEL_MARKDOWN.matcher(line).matches();
    }

    private static void appendSection(List<String> sections, String section) {
        String normalized = section.strip();
        if (!normalized.isEmpty()) {
            sections.add(normalized);
        }
    }
}
