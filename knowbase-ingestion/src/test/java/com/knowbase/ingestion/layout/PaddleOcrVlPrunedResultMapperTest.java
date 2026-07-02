package com.knowbase.ingestion.layout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowbase.ingestion.StructuralBlock;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaddleOcrVlPrunedResultMapperTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void mapsPrunedResultBlocksWithBboxAndReadingOrder() throws Exception {
        String json = """
                {
                  "parsing_res_list": [
                    {
                      "block_label": "doc_title",
                      "block_content": "Quarterly Report",
                      "block_bbox": [72, 40, 300, 80],
                      "block_order": 0
                    },
                    {
                      "block_label": "text",
                      "block_content": "Revenue increased by 12%.",
                      "block_bbox": [72, 120, 520, 160],
                      "block_order": 1
                    },
                    {
                      "block_label": "table",
                      "block_content": "| Name | Score |\\n| --- | --- |\\n| Alice | 95 |",
                      "block_bbox": [72, 200, 520, 320],
                      "block_order": 2
                    }
                  ]
                }
                """;
        LayoutPageRequest request = new LayoutPageRequest(
                new byte[] {1},
                "image/png",
                1,
                612,
                792,
                "memory://page.png",
                Map.of()
        );
        List<StructuralBlock> blocks = PaddleOcrVlPrunedResultMapper.fromPrunedResult(
                MAPPER.readTree(json),
                request,
                "paddleocr-vl",
                "PaddleOCR-VL-1.6"
        );

        assertEquals(3, blocks.size());
        assertTrue(blocks.stream().anyMatch(block -> "heading".equals(block.blockType())));
        assertTrue(blocks.stream().anyMatch(block -> "table_row".equals(block.blockType())));
        StructuralBlock body = blocks.stream()
                .filter(block -> block.content().contains("Revenue"))
                .findFirst()
                .orElseThrow();
        assertEquals("paddle-layout", body.metadata().get("bboxSource"));
        assertTrue(body.metadata().get("bbox") instanceof List<?> bbox && bbox.size() == 4);
        assertEquals(1, body.metadata().get("readingOrder"));
    }

    @Test
    void mergesBboxesOntoMarkdownBlocksByOrder() throws Exception {
        String json = """
                {
                  "parsing_res_list": [
                    {
                      "block_content": "Title",
                      "block_bbox": [10, 10, 100, 40],
                      "block_order": 0
                    }
                  ]
                }
                """;
        List<StructuralBlock> markdownBlocks = List.of(new StructuralBlock(
                "heading",
                1,
                "Title",
                0,
                Map.of("pageNumber", 1, "bboxSource", "unavailable")
        ));
        List<StructuralBlock> merged = PaddleOcrVlPrunedResultMapper.mergeBboxesOntoMarkdownBlocks(
                markdownBlocks,
                MAPPER.readTree(json),
                792d
        );
        assertEquals("paddle-layout", merged.getFirst().metadata().get("bboxSource"));
        assertTrue(merged.getFirst().metadata().containsKey("bbox"));
    }
}
