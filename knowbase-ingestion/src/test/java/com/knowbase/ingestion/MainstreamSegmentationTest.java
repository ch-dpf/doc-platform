package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.status.ContentFamily;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainstreamSegmentationTest {

    private final StructureSegmenter segmenter = new StructureSegmenter();

    @Test
    void manualOutlineProducesMultipleSections() {
        List<StructuralBlock> blocks = new ArrayList<>();
        blocks.add(StructuralBlock.heading(1, "一、系统概述", 0));
        blocks.add(StructuralBlock.heading(2, "1.1 开发目的", 1));
        blocks.add(StructuralBlock.paragraph("平台旨在构建高效智能的浮标运维平台。", 2));
        blocks.add(StructuralBlock.heading(2, "1.2 技术特点", 3));
        blocks.add(StructuralBlock.paragraph("基于 Spring Boot 与 Vue。", 4));
        blocks.add(StructuralBlock.heading(1, "二、系统部署与启动", 5));
        blocks.add(StructuralBlock.paragraph("部署说明正文。", 6));
        blocks = StructureParsingSupport.enrichHeadingPathsPublic(blocks);

        ParsedDocument document = new ParsedDocument(
                "memory://manual.docx",
                "Manual",
                StructureParsingSupport.blocksToText(blocks),
                ContentFamily.RICH_TEXT,
                Map.of(),
                blocks
        );
        List<StructuralSegment> segments = segmenter.segment(document, defaultProfile());
        assertTrue(segments.size() >= 3);
    }

    @Test
    void shortSectionSkipsInnerSplit() {
        CharacterWindowChunker chunker = new CharacterWindowChunker();
        List<String> windows = chunker.chunk(
                List.of("一、概述\n\n短正文。"),
                SegmentationOptionsSupport.SMART_CHUNK_MAX_CHARS,
                SegmentationOptionsSupport.SMART_CHUNK_OVERLAP_CHARS,
                80
        );
        assertEquals(1, windows.size());
    }

    @Test
    void formLikeBlobSplitsNearFiveHundredChars() {
        List<String> rows = new ArrayList<>();
        rows.add("软著撰写标准要求信息表");
        for (int i = 0; i < 24; i++) {
            rows.add("字段" + i + " | " + "值".repeat(60));
        }
        CharacterWindowChunker chunker = new CharacterWindowChunker();
        List<String> windows = chunker.chunk(
                rows,
                SegmentationOptionsSupport.SMART_CHUNK_MAX_CHARS,
                SegmentationOptionsSupport.SMART_CHUNK_OVERLAP_CHARS,
                80
        );
        assertTrue(windows.size() >= 4 && windows.size() <= 8);
    }

    private static DocumentProfile defaultProfile() {
        return new DocumentProfile(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "default_docx",
                ContentFamily.RICH_TEXT,
                "docx-structure",
                "structure_token_window",
                null,
                Map.of(),
                Map.of(),
                true
        );
    }
}
