package com.knowbase.ingestion.table;

import com.knowbase.ingestion.StructuralBlock;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TableParseConfidenceAggregatorTest {

    @Test
    void highScoreForHealthyCsvLikeBlocks() {
        TableParseConfidence confidence = TableParseConfidenceAggregator.aggregate(List.of(
                dataRow("DATA"),
                dataRow("DATA"),
                headerRow()
        ));
        assertTrue(confidence.score() >= 0.8d);
        assertTrue(confidence.dataRows() >= 2);
    }

    @Test
    void lowScoreForCoordinateFallback() {
        TableParseConfidence confidence = TableParseConfidenceAggregator.aggregate(List.of(
                dataRow("COORDINATE"),
                dataRow("COORDINATE"),
                dataRow("COORDINATE")
        ));
        assertTrue(confidence.score() < 0.8d);
        assertTrue(confidence.reasons().contains("high_coordinate_fallback"));
    }

    private static StructuralBlock dataRow(String role) {
        return new StructuralBlock("table_row", 0, "x", 0, Map.of("rowRole", role));
    }

    private static StructuralBlock headerRow() {
        return new StructuralBlock("table_row", 0, "h", 1, Map.of("rowRole", "HEADER"));
    }
}
