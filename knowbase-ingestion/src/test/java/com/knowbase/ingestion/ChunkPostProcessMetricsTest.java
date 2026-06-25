package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentChunk;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkPostProcessMetricsTest {

    @Test
    void notAppliedPreservesCounts() {
        List<DocumentChunk> chunks = List.of(
                chunk("row", Map.of("chunkRole", "flat")),
                chunk("row2", Map.of("chunkRole", "flat"))
        );

        ChunkPostProcessMetrics metrics = ChunkPostProcessMetrics.notApplied(chunks);

        assertFalse(metrics.applied());
        assertEquals(2, metrics.beforeCount());
        assertEquals(2, metrics.afterCount());
        assertEquals(0, metrics.summariesAdded());
        assertEquals(0, metrics.rowsMerged());
        assertEquals(0, metrics.deduplicated());
    }

    @Test
    void computeDetectsSummaryRowsMergedAndDedup() {
        List<DocumentChunk> before = List.of(
                chunk("a", Map.of("chunkRole", "flat")),
                chunk("b", Map.of("chunkRole", "flat")),
                chunk("c", Map.of("chunkRole", "flat")),
                chunk("dup", Map.of("chunkRole", "flat"))
        );
        List<DocumentChunk> after = List.of(
                chunk("summary", Map.of("chunkRole", "document_summary"), "document_summary"),
                chunk("group", Map.of("chunkRole", "table_row_group"))
        );

        ChunkPostProcessMetrics metrics = ChunkPostProcessMetrics.compute(before, after);

        assertTrue(metrics.applied());
        assertEquals(4, metrics.beforeCount());
        assertEquals(2, metrics.afterCount());
        assertEquals(1, metrics.summariesAdded());
        assertEquals(1, metrics.rowsMerged());
        assertEquals(1, metrics.deduplicated());
    }

    private static DocumentChunk chunk(String content, Map<String, Object> metadata) {
        return chunk(content, metadata, "paragraph");
    }

    private static DocumentChunk chunk(String content, Map<String, Object> metadata, String boundaryType) {
        return new DocumentChunk(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                content,
                8,
                "approx-test",
                "1",
                "bge-m3",
                boundaryType,
                null,
                metadata
        );
    }
}
