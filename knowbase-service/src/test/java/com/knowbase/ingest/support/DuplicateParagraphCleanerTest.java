package com.knowbase.ingest.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DuplicateParagraphCleanerTest {

    @Test
    void removesDuplicateSingleLineParagraphs() {
        String input = "示例段落一。\n示例段落一。\n示例段落二，用于预览分块效果。";
        assertEquals(
                "示例段落一。\n\n示例段落二，用于预览分块效果。",
                DuplicateParagraphCleaner.removeDuplicates(input));
    }

    @Test
    void removesDuplicateBlankLineSeparatedParagraphs() {
        String input = "第一段。\n\n第一段。\n\n第二段。";
        assertEquals("第一段。\n\n第二段。", DuplicateParagraphCleaner.removeDuplicates(input));
    }

    @Test
    void idempotentAfterDedup() {
        String once = DuplicateParagraphCleaner.removeDuplicates("A\n\nA\n\nB");
        assertEquals(once, DuplicateParagraphCleaner.removeDuplicates(once));
    }
}
