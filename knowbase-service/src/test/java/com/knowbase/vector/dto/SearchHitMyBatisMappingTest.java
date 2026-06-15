package com.knowbase.vector.dto;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchHitMyBatisMappingTest {

    @Test
    void nullableJdbcRowDefaultsNumericFields() {
        UUID chunkId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        SearchHit hit = new SearchHit(
                chunkId, docId, "demo", null, null, "content", null,
                null, null, null, null, null, null, null, null, null);

        assertEquals(0, hit.version());
        assertEquals(0, hit.chunkIndex());
        assertEquals(0.0, hit.score());
    }
}
