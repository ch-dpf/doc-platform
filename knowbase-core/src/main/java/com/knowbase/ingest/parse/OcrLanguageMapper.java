package com.knowbase.ingest.parse;

/**
 * 将库级 defaultLanguage 映射为 Tesseract 语言包标识。
 */
public final class OcrLanguageMapper {

    private OcrLanguageMapper() {
    }

    public static String toTesseractLanguage(String defaultLanguage, String fallback) {
        if (defaultLanguage == null || defaultLanguage.isBlank()) {
            return fallback;
        }
        String lang = defaultLanguage.trim().toLowerCase();
        if (lang.startsWith("zh")) {
            return "chi_sim+eng";
        }
        if (lang.startsWith("en")) {
            return "eng";
        }
        if (lang.startsWith("ja")) {
            return "jpn+eng";
        }
        if (lang.startsWith("ko")) {
            return "kor+eng";
        }
        return fallback;
    }
}
