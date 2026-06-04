package com.docplatform.ingest.support;

import com.docplatform.library.config.TextNormalizationSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

final class TextNormalizationEngine {

    private TextNormalizationEngine() {
    }

    static String normalize(String raw, TextNormalizationSettings settings) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        if (settings == null || !settings.isEnabled()) {
            return raw.trim();
        }

        String text = raw.replace("\r\n", "\n").replace('\r', '\n');
        if (settings.isRemoveControlChars()) {
            text = removeControlChars(text);
        }
        if (settings.isNormalizeUnicodeSpaces()) {
            text = normalizeSpaces(text);
        }

        String[] lines = text.split("\n", -1);
        List<String> kept = new ArrayList<>(lines.length);
        List<Pattern> dropPatterns = compileDropPatterns(settings);
        for (String line : lines) {
            String processed = settings.isTrimLines() ? line.strip() : line;
            if (processed.isEmpty()) {
                kept.add("");
                continue;
            }
            if (settings.isDropNoiseLines() && shouldDropLine(processed, settings, dropPatterns)) {
                continue;
            }
            kept.add(processed);
        }

        text = joinLines(kept);
        if (settings.isCollapseBlankLines()) {
            text = text.replaceAll("\n{3,}", "\n\n");
        }
        return text.strip();
    }

    private static boolean shouldDropLine(String line, TextNormalizationSettings settings, List<Pattern> patterns) {
        if (line.length() < settings.getMinLineLength()) {
            return true;
        }
        for (Pattern pattern : patterns) {
            if (pattern.matcher(line).matches()) {
                return true;
            }
        }
        return false;
    }

    private static List<Pattern> compileDropPatterns(TextNormalizationSettings settings) {
        List<Pattern> compiled = new ArrayList<>();
        if (settings.getLinePatternsToDrop() == null) {
            return compiled;
        }
        for (String regex : settings.getLinePatternsToDrop()) {
            if (regex != null && !regex.isBlank()) {
                compiled.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
            }
        }
        return compiled;
    }

    private static String joinLines(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(lines.get(i));
        }
        return sb.toString();
    }

    private static String removeControlChars(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n' || c == '\t' || c >= 0x20) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String normalizeSpaces(String text) {
        return text
                .replace('\u00a0', ' ')
                .replace('\u3000', ' ')
                .replaceAll(" +", " ");
    }
}
