package com.docplatform.ingest.support;

import com.docplatform.ingest.config.TextNormalizationProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ParsedTextNormalizerTest {

    @Test
    void collapsesBlankLinesAndDropsPageNumbers() {
        TextNormalizationProperties props = new TextNormalizationProperties();
        ParsedTextNormalizer normalizer = new ParsedTextNormalizer(props);
        String raw = "第一段内容\r\n\r\n\r\n\r\n第二段内容\r\n12\r\n第三段";
        String out = normalizer.normalize(raw);
        assertEquals("第一段内容\n\n第二段内容\n第三段", out);
        assertFalse(out.contains("\n12\n"));
    }

    @Test
    void disabledReturnsTrimmedOnly() {
        TextNormalizationProperties props = new TextNormalizationProperties();
        props.setEnabled(false);
        ParsedTextNormalizer normalizer = new ParsedTextNormalizer(props);
        assertEquals("x", normalizer.normalize("  x  "));
    }
}
