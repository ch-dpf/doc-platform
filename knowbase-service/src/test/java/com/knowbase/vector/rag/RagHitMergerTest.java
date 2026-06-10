package com.knowbase.vector.rag;

import com.knowbase.vector.dto.SearchHit;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RagHitMergerTest {

    @Test
    void mergeSortsByScoreDescending() {
        UUID docId = UUID.randomUUID();
        UUID shared = UUID.randomUUID();
        SearchHit low = new SearchHit(shared, docId, "t", 1, 0, "a", 0.3);
        SearchHit high = new SearchHit(shared, docId, "t", 1, 0, "b", 0.8);
        SearchHit other = new SearchHit(UUID.randomUUID(), docId, "t", 1, 1, "c", 0.6);

        List<SearchHit> merged = RagHitMerger.merge(List.of(low), List.of(high, other), 2);

        assertEquals(2, merged.size());
        assertEquals(0.8, merged.get(0).score(), 0.001);
        assertEquals(0.6, merged.get(1).score(), 0.001);
        assertEquals("b", merged.get(0).content());
    }
}
