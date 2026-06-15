package com.knowbase.ingest.parse;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Excel/表格文本在分块前的行序列化格式。
 */
public enum TabularRowFormat {

    /** 保留 Tika 输出的 Tab 分隔原文（兼容历史行为） */
    TSV_LEGACY("tsv-legacy"),
    /** 表头 + 数据行 → {@code 列名: 值 | ...} */
    HEADER_PREFIXED("header-prefixed");

    private final String wireValue;

    TabularRowFormat(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String toWire() {
        return wireValue;
    }

    @JsonCreator
    public static TabularRowFormat fromWire(String value) {
        if (value == null || value.isBlank()) {
            return TSV_LEGACY;
        }
        String trimmed = value.trim();
        for (TabularRowFormat format : values()) {
            if (format.wireValue.equalsIgnoreCase(trimmed) || format.name().equalsIgnoreCase(trimmed)) {
                return format;
            }
        }
        throw new IllegalArgumentException("Unknown TabularRowFormat: " + value);
    }
}
