package com.knowbase.ingestion.layout;

/**
 * Prompt templates for Ollama vision layout / table detection (RAG-style structured JSON output).
 */
public final class OllamaLayoutPrompts {

    public static final String LAYOUT_TABLE_SYSTEM = """
            You are a document layout analyzer. Inspect the page image and return ONLY valid JSON.
            Detect text blocks and tables including ruled grids, borderless aligned-column tables, and nested tables.
            Use PDF-style bbox [x, y, width, height] with origin bottom-left; page height is provided.
            tableType must be one of: ruled, borderless, nested, stream.
            """;

    public static final String READING_ORDER_SYSTEM = """
            You are a reading-order model for document layout blocks.
            Given block metadata (page, bbox, type, content preview), return ONLY valid JSON with reading order.
            Respect multi-column layout: read column 0 top-to-bottom before column 1.
            """;

    private OllamaLayoutPrompts() {
    }

    public static String layoutTableUserPrompt(int pageNumber, double pageWidth, double pageHeight) {
        return """
                Analyze page %d (width=%.1f, height=%.1f).
                Return JSON:
                {
                  "blocks": [
                    {"type":"paragraph|heading|table_row","content":"...","bbox":[x,y,w,h],
                     "readingOrder":0,"columnIndex":0,"columnCount":1}
                  ],
                  "tables": [
                    {"tableRegionId":0,"tableType":"ruled|borderless|nested|stream","nestedDepth":0,
                     "bbox":[x,y,w,h],
                     "rows":[{"cells":["c1","c2"],"cellBboxes":[[x,y,w,h],[x,y,w,h]]}]}
                  ]
                }
                """.formatted(pageNumber, pageWidth, pageHeight);
    }

    public static String readingOrderUserPrompt(String blocksJson) {
        return """
                Order these blocks for human reading. Input blocks (index is array position):
                %s
                Return JSON: {"orders":[{"index":0,"readingOrder":0}]}
                """.formatted(blocksJson);
    }
}
