package com.knowbase.library.config;

import com.knowbase.ingest.config.TextNormalizationProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

/**
 * 向量库级文本清洗规则（持久化在 config_json.textNormalization）。
 */
@Schema(description = "文本规范化规则")
public class TextNormalizationSettings {

    @Schema(description = "是否启用", example = "true")
    private boolean enabled = true;
    @Schema(description = "合并连续空行", example = "true")
    private boolean collapseBlankLines = true;
    @Schema(description = "行首尾去空白", example = "true")
    private boolean trimLines = true;
    @Schema(description = "移除控制字符", example = "true")
    private boolean removeControlChars = true;
    @Schema(description = "统一 Unicode 空格", example = "true")
    private boolean normalizeUnicodeSpaces = true;
    @Schema(description = "丢弃噪声行", example = "true")
    private boolean dropNoiseLines = true;
    @Schema(description = "最短保留行长度", example = "2")
    private int minLineLength = 2;
    @Schema(description = "行级清洗正则：整行命中则删除（如页码行）")
    private List<String> linePatternsToDrop = new ArrayList<>(List.of(
            "^\\d{1,4}$",
            "^第\\s*\\d+\\s*页$",
            "^Page\\s+\\d+\\s+of\\s+\\d+$",
            "^-{3,}$",
            "^_{3,}$"));

    public static TextNormalizationSettings fromGlobal(TextNormalizationProperties global) {
        TextNormalizationSettings s = new TextNormalizationSettings();
        s.setEnabled(global.isEnabled());
        s.setCollapseBlankLines(global.isCollapseBlankLines());
        s.setTrimLines(global.isTrimLines());
        s.setRemoveControlChars(global.isRemoveControlChars());
        s.setNormalizeUnicodeSpaces(global.isNormalizeUnicodeSpaces());
        s.setDropNoiseLines(global.isDropNoiseLines());
        s.setMinLineLength(global.getMinLineLength());
        s.setLinePatternsToDrop(new ArrayList<>(global.getLinePatternsToDrop()));
        return s;
    }

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
