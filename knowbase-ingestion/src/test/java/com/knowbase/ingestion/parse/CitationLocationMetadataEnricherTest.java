package com.knowbase.ingestion.parse;

import com.knowbase.ingestion.StructuralBlock;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CitationLocationMetadataEnricherTest {

    @Test
    void derivesPrimaryCellRefFromCellCoordinates() {
        StructuralBlock block = CitationLocationMetadataEnricher.apply(new StructuralBlock(
                "table_row",
                0,
                "张三 | 研发",
                0,
                Map.of(
                        "sheetName", "Metrics",
                        "rowIndex", 1,
                        "cellCoordinates", List.of(
                                Map.of("rowIndex", 1, "columnIndex", 0, "cellRef", "A2", "value", "张三"),
                                Map.of("rowIndex", 1, "columnIndex", 1, "cellRef", "B2", "value", "研发")
                        )
                )
        ));

        assertEquals("A2", block.metadata().get("primaryCellRef"));
        assertEquals(List.of("A2", "B2"), block.metadata().get("cellRefs"));
        assertEquals(0, block.metadata().get("columnIndex"));
    }

    @Test
    void evidenceHintIncludesSheetCellRef() {
        StructuralBlock block = EvidenceAssetHintEnricher.apply(new StructuralBlock(
                "table_row",
                0,
                "data",
                0,
                Map.of(
                        "sheetName", "Metrics",
                        "rowIndex", 2,
                        "primaryCellRef", "C3",
                        "cellRefs", List.of("C3", "D3")
                )
        ));

        @SuppressWarnings("unchecked")
        Map<String, Object> hint = (Map<String, Object>) block.metadata().get("evidenceAssetHint");
        assertNotNull(hint);
        assertEquals("sheet_row", hint.get("kind"));
        assertEquals("C3", hint.get("primaryCellRef"));
        assertEquals(List.of("C3", "D3"), hint.get("cellRefs"));
    }
}
