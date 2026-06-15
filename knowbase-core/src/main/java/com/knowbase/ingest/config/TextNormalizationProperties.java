package com.knowbase.ingest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "ingest.text-normalization")
public class TextNormalizationProperties {

    private boolean enabled = true;
    private boolean collapseBlankLines = true;
    private boolean trimLines = true;
    private boolean removeControlChars = true;
    private boolean normalizeUnicodeSpaces = true;
    private boolean dropNoiseLines = true;
    private int minLineLength = 2;
    private List<String> linePatternsToDrop = new ArrayList<>(List.of(
            "^\\d{1,4}$",
            "^第\\s*\\d+\\s*页$",
            "^Page\\s+\\d+\\s+of\\s+\\d+$",
            "^-{3,}$",
            "^_{3,}$"));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isCollapseBlankLines() {
        return collapseBlankLines;
    }

    public void setCollapseBlankLines(boolean collapseBlankLines) {
        this.collapseBlankLines = collapseBlankLines;
    }

    public boolean isTrimLines() {
        return trimLines;
    }

    public void setTrimLines(boolean trimLines) {
        this.trimLines = trimLines;
    }

    public boolean isRemoveControlChars() {
        return removeControlChars;
    }

    public void setRemoveControlChars(boolean removeControlChars) {
        this.removeControlChars = removeControlChars;
    }

    public boolean isNormalizeUnicodeSpaces() {
        return normalizeUnicodeSpaces;
    }

    public void setNormalizeUnicodeSpaces(boolean normalizeUnicodeSpaces) {
        this.normalizeUnicodeSpaces = normalizeUnicodeSpaces;
    }

    public boolean isDropNoiseLines() {
        return dropNoiseLines;
    }

    public void setDropNoiseLines(boolean dropNoiseLines) {
        this.dropNoiseLines = dropNoiseLines;
    }

    public int getMinLineLength() {
        return minLineLength;
    }

    public void setMinLineLength(int minLineLength) {
        this.minLineLength = minLineLength;
    }

    public List<String> getLinePatternsToDrop() {
        return linePatternsToDrop;
    }

    public void setLinePatternsToDrop(List<String> linePatternsToDrop) {
        this.linePatternsToDrop = linePatternsToDrop;
    }
}
