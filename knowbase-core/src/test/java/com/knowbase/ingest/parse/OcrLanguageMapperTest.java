package com.knowbase.ingest.parse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OcrLanguageMapperTest {

    @Test
    void mapsChineseLocale() {
        assertEquals("chi_sim+eng", OcrLanguageMapper.toTesseractLanguage("zh-CN", "eng"));
    }

    @Test
    void mapsEnglishLocale() {
        assertEquals("eng", OcrLanguageMapper.toTesseractLanguage("en-US", "chi_sim+eng"));
    }

    @Test
    void usesFallbackForUnknownLocale() {
        assertEquals("chi_sim+eng", OcrLanguageMapper.toTesseractLanguage("fr-FR", "chi_sim+eng"));
    }
}
