package com.knowbase.ingestion.pdf;

import java.util.List;

public record PdfTableRowInput(
        int pageNumber,
        int readingOrder,
        int columnIndex,
        int columnCount,
        String content,
        float minX,
        float y,
        float width,
        float height,
        List<Float> cellBoundaryX
) {
    public PdfTableRowInput(
            int pageNumber,
            int readingOrder,
            int columnIndex,
            int columnCount,
            String content,
            float minX,
            float y,
            float width,
            float height
    ) {
        this(pageNumber, readingOrder, columnIndex, columnCount, content, minX, y, width, height, List.of());
    }

    public List<Float> cellBoundaryX() {
        return cellBoundaryX == null ? List.of() : cellBoundaryX;
    }
}
