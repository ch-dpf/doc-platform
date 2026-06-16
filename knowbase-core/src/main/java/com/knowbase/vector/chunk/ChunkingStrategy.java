package com.knowbase.vector.chunk;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "分块策略", enumAsRef = true)
public enum ChunkingStrategy {
    /** 按文件类型应用代码默认策略（Word/Markdown→按标题，PDF/TXT/Excel→按段落） */
    AUTO("auto"),
    /** 优先按段落（空行）切分，过长段落再按带重叠的字符窗口切（类 recursive character） */
    PARAGRAPH_FIRST("paragraph-first"),
    /** 语义分块 */
    SEMANTIC("semantic"),
    /** 按标题层级切分 */
    HEADING_LEVEL("heading-level"),
    /** 固定字符长度滑动窗口 */
    FIXED_CHAR("fixed-char");

    private final String wireValue;

    ChunkingStrategy(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String toWire() {
        return wireValue;
    }

    @JsonCreator
    public static ChunkingStrategy fromWire(String value) {
        if (value == null || value.isBlank()) {
            return PARAGRAPH_FIRST;
        }
        String trimmed = value.trim();
        if ("tabular-row".equalsIgnoreCase(trimmed)) {
            return PARAGRAPH_FIRST;
        }
        for (ChunkingStrategy strategy : values()) {
            if (strategy.wireValue.equalsIgnoreCase(trimmed)
                    || strategy.name().equalsIgnoreCase(trimmed)
                    || strategy.name().equalsIgnoreCase(trimmed.replace('-', '_'))) {
                return strategy;
            }
        }
        throw new IllegalArgumentException("Unknown ChunkingStrategy: " + value);
    }
}
