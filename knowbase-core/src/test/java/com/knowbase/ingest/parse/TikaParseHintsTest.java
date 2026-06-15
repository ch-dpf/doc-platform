package com.knowbase.ingest.parse;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TikaParseHintsTest {

    @Test
    void mapsFixedEncodingForChineseWhenAutoDetectDisabled() {
        assertEquals("GB18030", TikaEncodingMapper.fixedEncodingForLanguage("zh-CN"));
        assertEquals("UTF-8", TikaEncodingMapper.fixedEncodingForLanguage("en-US"));
    }

    @Test
    void appliesLanguageAndEncodingHints() {
        Metadata metadata = new Metadata();
        DocumentParseOptions options = new DocumentParseOptions(
                false,
                "chi_sim+eng",
                false,
                "en-US",
                "UTF-8",
                TableExtractionMode.TEXT_ONLY,
                ImageExtractionMode.SKIP,
                FormulaExtractionMode.SKIP);

        TikaMetadataHints.apply(metadata, options);

        assertEquals("en-US", metadata.get(TikaCoreProperties.LANGUAGE));
        assertEquals("en-US", metadata.get(Metadata.CONTENT_LANGUAGE));
        assertEquals("UTF-8", metadata.get(Metadata.CONTENT_ENCODING));
    }

    @Test
    void skipsEncodingHintWhenAutoDetectEnabled() {
        Metadata metadata = new Metadata();
        DocumentParseOptions options = new DocumentParseOptions(
                false,
                "chi_sim+eng",
                true,
                "zh-CN",
                null,
                TableExtractionMode.TEXT_ONLY,
                ImageExtractionMode.SKIP,
                FormulaExtractionMode.SKIP);

        TikaMetadataHints.apply(metadata, options);

        assertEquals("zh-CN", metadata.get(TikaCoreProperties.LANGUAGE));
        assertNull(metadata.get(Metadata.CONTENT_ENCODING));
    }
}
