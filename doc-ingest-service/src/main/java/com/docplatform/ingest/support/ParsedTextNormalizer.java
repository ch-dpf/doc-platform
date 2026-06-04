package com.docplatform.ingest.support;

import com.docplatform.ingest.config.TextNormalizationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class ParsedTextNormalizer {

    private final TextNormalizationProperties properties;
    private volatile List<Pattern> dropPatterns;

    public ParsedTextNormalizer(TextNormalizationProperties properties) {
        this.properties = properties;
    }

    public String normalize(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        if (!properties.isEnabled()) {
            return raw.trim();
        }

        String text = raw.replace("\r\n", "\n").replace('\r', '\n');
        if (properties.isRemoveControlChars()) {
            text = removeControlChars(text);
        }
        if (properties.isNormalizeUnicodeSpaces()) {
            text = normalizeSpaces(text);
        }

        String[] lines = text.split("\n", -1);
        List<String> kept = new ArrayList<>(lines.length);
        for (String line : lines) {
            String processed = properties.isTrimLines() ? line.strip() : line;
            if (processed.isEmpty()) {
                kept.add("");
                continue;
            }
            if (properties.isDropNoiseLines() && shouldDropLine(processed)) {
                continue;
            }
            kept.add(processed);
        }

        text = joinLines(kept);
        if (properties.isCollapseBlankLines()) {
            text = text.replaceAll("\n{3,}", "\n\n");
        }
        return text.strip();
    }

    private String joinLines(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(lines.get(i));
        }
        return sb.toString();
    }

    private boolean shouldDropLine(String line) {
        if (line.length() < properties.getMinLineLength()) {
            return true;
        }
        for (Pattern pattern : compiledDropPatterns()) {
            if (pattern.matcher(line).matches()) {
                return true;
            }
        }
        return false;
    }

    private List<Pattern> compiledDropPatterns() {
        List<Pattern> cached = dropPatterns;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (dropPatterns == null) {
                List<Pattern> compiled = new ArrayList<>();
                for (String regex : properties.getLinePatternsToDrop()) {
                    if (regex != null && !regex.isBlank()) {
                        compiled.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
                    }
                }
                dropPatterns = List.copyOf(compiled);
            }
            return dropPatterns;
        }
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
