package com.knowbase.vector.chunk;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeadingLevelChunkerTest {

    @Test
    void splitsMarkdownAndChineseHeadings() {
        String text =
                """
                # 概述
                第一段内容。

                ## 细节
                第二段内容。
                """;
        List<String> sections = HeadingLevelChunker.splitSections(text);
        assertEquals(2, sections.size());
        assertTrue(sections.get(0).contains("概述"));
        assertTrue(sections.get(1).contains("细节"));
    }

    @Test
    void splitsNumberedHeadings() {
        String text = "1. 背景\n背景说明。\n2. 方案\n方案说明。";
        List<String> sections = HeadingLevelChunker.splitSections(text);
        assertEquals(2, sections.size());
        assertTrue(sections.get(0).startsWith("1. 背景"));
    }

    @Test
    void splitTopLevelKeepsSubheadingsInSameSection() {
        String text =
                """
                # 向量检索
                第一段。

                ## 子节
                第二段。

                # 另一主题
                第三段。
                """;
        List<String> sections = HeadingLevelChunker.splitTopLevelSections(text);
        assertEquals(2, sections.size());
        assertTrue(sections.get(0).contains("子节"));
        assertTrue(sections.get(1).contains("另一主题"));
    }
}
