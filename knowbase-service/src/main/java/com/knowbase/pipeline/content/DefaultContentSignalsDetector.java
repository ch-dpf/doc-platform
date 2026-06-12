package com.knowbase.pipeline.content;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 轻量启发式内容结构探测（无 ML）：标题密度、代码围栏、表格行占比、短文判定。
 */
@Component
public class DefaultContentSignalsDetector implements ContentSignalsDetector {

    private static final Pattern MARKDOWN_HEADING = Pattern.compile("^#{1,6}\\s+\\S", Pattern.MULTILINE);
    private static final Pattern CN_SECTION_HEADING =
            Pattern.compile("^第[一二三四五六七八九十百千0-9]+[章节条篇]\\s*\\S?", Pattern.MULTILINE);
    private static final Pattern CODE_FENCE = Pattern.compile("```");

    @Override
    public ContentSignals detect(ContentFamily family, String mimeType, String text) {
        ContentFamily resolvedFamily = family != null ? family : ContentFamily.UNKNOWN;
        if (text == null || text.isBlank()) {
            return ContentSignals.empty(resolvedFamily);
        }
        String normalized = text.replace("\r\n", "\n");
        String[] lines = normalized.split("\n", -1);

        ContentSignals signals = new ContentSignals();
        signals.setContentFamily(resolvedFamily);
        signals.setTextLength(normalized.length());

        int headingLines = 0;
        int tabularLines = 0;
        int nonBlankLines = 0;
        for (String line : lines) {
            String trimmed = line.strip();
            if (trimmed.isEmpty()) {
                continue;
            }
            nonBlankLines++;
            if (isHeadingLine(trimmed)) {
                headingLines++;
            }
            if (trimmed.contains("\t") || trimmed.matches(".*\\|.*\\|.*")) {
                tabularLines++;
            }
        }

        signals.setHeadingLineCount(headingLines);
        signals.setHeadingLineRatio(nonBlankLines == 0 ? 0 : (double) headingLines / nonBlankLines);
        signals.setMarkdownHeadings(MARKDOWN_HEADING.matcher(normalized).find());
        signals.setCodeFences(CODE_FENCE.matcher(normalized).find());
        signals.setTabularLineRatio(nonBlankLines == 0 ? 0 : (double) tabularLines / nonBlankLines);
        signals.setShortDocument(normalized.length() < ContentSignals.SHORT_DOCUMENT_CHARS);
        return signals;
    }

    private static boolean isHeadingLine(String trimmed) {
        return MARKDOWN_HEADING.matcher(trimmed).matches()
                || CN_SECTION_HEADING.matcher(trimmed).matches();
    }
}
