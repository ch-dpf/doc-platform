package com.knowbase.vector.retrieval;

import com.knowbase.vector.dto.SearchHit;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HybridSearchFusionTest {

    private static final UUID DOC = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void mergesBothListsWithRrfAndPrefersDualMatches() {
        UUID shared = UUID.fromString("00000000-0000-0000-0000-000000000010");
        UUID vectorOnly = UUID.fromString("00000000-0000-0000-0000-000000000011");
        UUID keywordOnly = UUID.fromString("00000000-0000-0000-0000-000000000012");

        List<SearchHit> vectorHits = List.of(
                hit(shared, "shared chunk", 0.92),
                hit(vectorOnly, "vector only", 0.88));
        List<SearchHit> keywordHits = List.of(
                hit(shared, "shared chunk", 0.95),
                hit(keywordOnly, "keyword only", 0.80));

        List<SearchHit> merged = HybridSearchFusion.mergeByReciprocalRankFusion(vectorHits, keywordHits, 60, 2);

        assertEquals(2, merged.size());
        assertEquals(shared, merged.get(0).chunkId());
    }

    @Test
    void returnsVectorHitsWhenKeywordEmpty() {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000020");
        List<SearchHit> vectorHits = List.of(hit(id, "only vector", 0.7));

        List<SearchHit> merged = HybridSearchFusion.mergeByReciprocalRankFusion(vectorHits, List.of(), 60, 5);

        assertEquals(1, merged.size());
        assertEquals(id, merged.get(0).chunkId());
    }

    private static SearchHit hit(UUID chunkId, String content, double score) {
        return new SearchHit(chunkId, DOC, "demo", 1, 0, content, score);
    }
}
