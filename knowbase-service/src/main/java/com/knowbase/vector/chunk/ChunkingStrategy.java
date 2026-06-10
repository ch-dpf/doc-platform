package com.knowbase.vector.chunk;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ChunkingStrategy {
    /** 优先按段落（空行）切分，过长段落再按字符窗口切 */
    PARAGRAPH_FIRST("paragraph-first"),
    /** 语义分块 */
    SEMANTIC("semantic"),
    /** 按标题层级切分 */
    HEADING_LEVEL("heading-level"),
    /** 固定字符长度滑动窗口（兼容旧行为） */
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
