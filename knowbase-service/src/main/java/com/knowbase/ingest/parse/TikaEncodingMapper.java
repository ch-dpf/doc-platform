package com.knowbase.ingest.parse;

import java.nio.charset.StandardCharsets;

/**
 * 将库级 defaultLanguage 映射为 Tika 固定字符集（关闭自动识别编码时使用）。
 */
public final class TikaEncodingMapper {

    private TikaEncodingMapper() {}

    public static String normalizeLanguageTag(String defaultLanguage) {
        if (defaultLanguage == null || defaultLanguage.isBlank()) {
            return "zh-CN";
        }
        return defaultLanguage.trim();
    }

    public static String fixedEncodingForLanguage(String defaultLanguage) {
        if (defaultLanguage == null || defaultLanguage.isBlank()) {
            return StandardCharsets.UTF_8.name();
        }
        if (defaultLanguage.trim().toLowerCase().startsWith("zh")) {
            return "GB18030";
        }
        return StandardCharsets.UTF_8.name();
    }
}
