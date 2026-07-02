package com.knowbase.ingestion.layout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OllamaLayoutResponseMapperTest {

    @Test
    void mapsRuledTableAndParagraphBlocks() {
        String json = """
                {
                  "blocks": [
                    {"type":"paragraph","content":"Intro text","bbox":[72,700,400,20],"readingOrder":0,"columnIndex":0,"columnCount":1}
                  ],
                  "tables": [
                    {
                      "tableRegionId": 0,
                      "tableType": "ruled",
                      "nestedDepth": 0,
                      "bbox": [72,500,400,80],
                      "rows": [
                        {"cells":["Name","Age"],"cellBboxes":[[72,560,120,18],[220,560,80,18]]},
                        {"cells":["Alice","30"],"cellBboxes":[[72,540,120,18],[220,540,80,18]]}
                      ]
                    }
                  ]
                }
                """;
        LayoutPageRequest request = new LayoutPageRequest(
                new byte[0],
                "image/png",
                1,
                612,
                792,
                "memory://sample.pdf",
                java.util.Map.of()
        );
        OllamaLayoutResponseMapper.MappedPage mapped = OllamaLayoutResponseMapper.fromJson(
                json,
                request,
                OllamaLayoutTableProvider.PROVIDER_CODE,
                "llama3.2-vision"
        );
        assertEquals(3, mapped.blocks().size());
        assertTrue(mapped.blocks().stream().anyMatch(block ->
                "table_row".equals(block.blockType()) && block.content().contains("Alice")));
        assertEquals(1, mapped.tableRegions().size());
        assertEquals("ollama-ruled", mapped.tableRegions().getFirst().detectionSource());
    }

    @Test
    void mapsNestedBorderlessTableType() {
        String json = """
                {
                  "tables": [
                    {
                      "tableRegionId": 1,
                      "tableType": "nested",
                      "nestedDepth": 1,
                      "bbox": [90,400,300,60],
                      "rows": [{"cells":["A","B"],"cellBboxes":[[90,440,80,16],[180,440,80,16]]}]
                    }
                  ]
                }
                """;
        LayoutPageRequest request = new LayoutPageRequest(
                new byte[0],
                "image/png",
                2,
                612,
                792,
                "memory://nested.pdf",
                java.util.Map.of()
        );
        OllamaLayoutResponseMapper.MappedPage mapped = OllamaLayoutResponseMapper.fromJson(
                json,
                request,
                OllamaLayoutTableProvider.PROVIDER_CODE,
                "llama3.2-vision"
        );
        assertEquals(1, mapped.blocks().size());
        assertEquals(1, mapped.blocks().getFirst().metadata().get("nestedTableDepth"));
        assertEquals("ollama-nested", mapped.tableRegions().getFirst().detectionSource());
    }
}
