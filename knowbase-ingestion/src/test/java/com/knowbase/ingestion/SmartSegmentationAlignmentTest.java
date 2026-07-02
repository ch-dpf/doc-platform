package com.knowbase.ingestion;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SmartSegmentationAlignmentTest {

    @Test
    void shortSectionStaysSingleChunkUnderFiveHundred() {
        List<String> paragraphs = List.of("一、概述", "短正文，不超过上限。");
        CharacterWindowChunker chunker = new CharacterWindowChunker();
        List<String> windows = chunker.chunk(
                paragraphs,
                SegmentationOptionsSupport.SMART_CHUNK_MAX_CHARS,
                SegmentationOptionsSupport.SMART_CHUNK_OVERLAP_CHARS,
                80
        );
        assertEquals(1, windows.size());
    }

    @Test
    void oversizedSectionSplitsWithMainstreamWindow() {
        List<String> paragraphs = List.of(
                "1、浮标运维与资产管理系统",
                "开发目的: " + "本".repeat(420),
                "技术特点：" + "技".repeat(250)
        );
        CharacterWindowChunker chunker = new CharacterWindowChunker();
        List<String> windows = chunker.chunk(
                paragraphs,
                SegmentationOptionsSupport.SMART_CHUNK_MAX_CHARS,
                SegmentationOptionsSupport.SMART_CHUNK_OVERLAP_CHARS,
                80
        );
        assertEquals(2, windows.size());
    }
}
