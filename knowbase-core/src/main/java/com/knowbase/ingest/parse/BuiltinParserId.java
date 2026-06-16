package com.knowbase.ingest.parse;

import java.util.Arrays;
import java.util.Optional;

/** 平台内置解析器标识（库级 parser_rules 引用）。 */
public enum BuiltinParserId {

    AUTO("auto", "自动（MIME 默认）", "按文件类型应用系统默认解析策略"),
    TIKA_PLAIN("tika-plain", "纯文本", "Tika 纯文本抽取，不做 HTML 结构化管道"),
    TIKA_STRUCTURED("tika-structured", "结构化文档", "HTML 管道 + 表格 structured，适合 Word/PDF 版式文档"),
    TIKA_OCR_AUTO("tika-ocr-auto", "扫描友好", "Tika 抽取不足时自动 OCR 回退"),
    EXCEL_STRUCTURED("excel-structured", "表格结构化", "Excel 优先 Markdown 表格（POI/Tika HTML 路径）"),
    TIKA_TABLE_TEXT("tika-table-text", "表格转文本", "表格扁平为 tab 分隔纯文本");

    private final String wire;
    private final String label;
    private final String description;

    BuiltinParserId(String wire, String label, String description) {
        this.wire = wire;
        this.label = label;
        this.description = description;
    }

    public String wire() {
        return wire;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    public static Optional<BuiltinParserId> fromWire(String value) {
        if (value == null || value.isBlank()) {
            return Optional.of(AUTO);
        }
        String normalized = value.trim().toLowerCase();
        return Arrays.stream(values()).filter(id -> id.wire.equals(normalized)).findFirst();
    }
}
