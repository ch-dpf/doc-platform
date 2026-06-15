package com.knowbase.pipeline.content;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * MIME 上层内容族群：解析/清洗/分块策略的粗粒度路由单元。
 * 具体 MIME → 族群映射见 {@link ContentFamilyResolver}。
 */
@Schema(description = "内容族群", enumAsRef = true)
public enum ContentFamily {

    /** 表格类：xls/xlsx/csv 等 */
    TABULAR("tabular"),
    /** 版式文档类：pdf/doc/docx 等 */
    DOCUMENT("document"),
    /** 纯文本类：txt/md 等 */
    PLAIN("plain"),
    /** 图片类：需 OCR 管线 */
    IMAGE("image"),
    /** 未识别 MIME，走平台基线 */
    UNKNOWN("unknown");

    private final String wireValue;

    ContentFamily(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String toWire() {
        return wireValue;
    }

    @JsonCreator
    public static ContentFamily fromWire(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        String trimmed = value.trim();
        for (ContentFamily family : values()) {
            if (family.wireValue.equalsIgnoreCase(trimmed) || family.name().equalsIgnoreCase(trimmed)) {
                return family;
            }
        }
        throw new IllegalArgumentException("Unknown ContentFamily: " + value);
    }
}
