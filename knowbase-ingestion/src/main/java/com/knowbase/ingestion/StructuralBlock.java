package com.knowbase.ingestion;

import java.util.Map;

public record StructuralBlock(
        String blockType,
        int level,
        String content,
        int ordinal,
        Map<String, Object> metadata
) {

    public StructuralBlock {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public StructuralBlock withContent(String newContent) {
        return new StructuralBlock(blockType, level, newContent, ordinal, metadata);
    }

    public static StructuralBlock heading(int level, String content, int ordinal) {
        return new StructuralBlock("heading", level, content, ordinal, Map.of("boundaryType", "section"));
    }

    public static StructuralBlock paragraph(String content, int ordinal) {
        return new StructuralBlock("paragraph", 0, content, ordinal, Map.of("boundaryType", "paragraph"));
    }

    public static StructuralBlock codeBlock(String content, int ordinal) {
        return new StructuralBlock("code_block", 0, content, ordinal, Map.of("boundaryType", "code_block"));
    }

    public static StructuralBlock listItem(String content, int ordinal, int listLevel) {
        return new StructuralBlock(
                "list_item",
                listLevel,
                content,
                ordinal,
                Map.of("boundaryType", "list_item", "listLevel", listLevel)
        );
    }

    public static StructuralBlock tableRow(String content, int ordinal, int rowIndex) {
        return new StructuralBlock(
                "table_row",
                0,
                content,
                ordinal,
                Map.of("boundaryType", "table_row", "rowIndex", rowIndex)
        );
    }

    public static StructuralBlock page(String content, int ordinal, int pageNumber) {
        return new StructuralBlock(
                "page",
                0,
                content,
                ordinal,
                Map.of("boundaryType", "page", "pageNumber", pageNumber)
        );
    }

    public static StructuralBlock domBlock(String tag, String content, int ordinal) {
        return new StructuralBlock(
                "dom_block",
                0,
                content,
                ordinal,
                Map.of("boundaryType", "dom_block", "tag", tag)
        );
    }
}
