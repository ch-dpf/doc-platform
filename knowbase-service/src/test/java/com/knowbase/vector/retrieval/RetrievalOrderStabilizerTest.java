package com.knowbase.vector.retrieval;

import com.knowbase.vector.dto.SearchHit;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RetrievalOrderStabilizerTest {

    @Test
    void stabilizesTiedScoresByDocAndChunk() {
        UUID docA = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID docB = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID chunk1 = UUID.fromString("00000000-0000-0000-0000-000000000011");
        UUID chunk2 = UUID.fromString("00000000-0000-0000-0000-000000000012");
        List<SearchHit> hits = List.of(
                new SearchHit(chunk2, docB, "demo", 1, 1, "b", 0.65),
                new SearchHit(chunk1, docA, "demo", 1, 0, "a", 0.65));

        List<SearchHit> stable = RetrievalOrderStabilizer.stabilize(hits);

        assertEquals(docA, stable.get(0).docId());
        assertEquals(docB, stable.get(1).docId());
    }
}
