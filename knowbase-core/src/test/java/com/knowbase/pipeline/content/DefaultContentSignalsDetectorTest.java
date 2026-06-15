package com.knowbase.pipeline.content;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultContentSignalsDetectorTest {

    private final DefaultContentSignalsDetector detector = new DefaultContentSignalsDetector();

    @Test
    void detectsMarkdownHeadingsAndCodeFences() {
        String body = "正文段落，包含足够长度以超过短文阈值。".repeat(150);
        String text = """
                # 第一章

                %s

                ```java
                int x = 1;
                ```
                """.formatted(body);
        ContentSignals signals = detector.detect(ContentFamily.PLAIN, "text/markdown", text);
        assertTrue(signals.isMarkdownHeadings());
        assertTrue(signals.isCodeFences());
        assertFalse(signals.isShortDocument());
    }

    @Test
    void marksShortDocument() {
        ContentSignals signals = detector.detect(ContentFamily.DOCUMENT, "application/pdf", "简短通知正文。");
        assertTrue(signals.isShortDocument());
    }
}
