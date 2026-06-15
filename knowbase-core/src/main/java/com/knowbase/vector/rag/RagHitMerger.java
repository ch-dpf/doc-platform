package com.knowbase.vector.rag;

import com.knowbase.vector.dto.SearchHit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class RagHitMerger {

    private RagHitMerger() {}

    public static List<SearchHit> merge(List<SearchHit> primary, List<SearchHit> secondary, int topK) {
        Map<UUID, SearchHit> byId = new LinkedHashMap<>();
        append(byId, primary);
        append(byId, secondary);
        List<SearchHit> merged = new ArrayList<>(byId.values());
        merged.sort(Comparator.comparingDouble(SearchHit::score).reversed());
        if (topK <= 0 || merged.size() <= topK) {
            return merged;
        }
        return List.copyOf(merged.subList(0, topK));
    }

    private static void append(Map<UUID, SearchHit> byId, List<SearchHit> hits) {
        if (hits == null) {
            return;
        }
        for (SearchHit hit : hits) {
            byId.merge(hit.chunkId(), hit, RagHitMerger::preferHigherScore);
        }
    }

    private static SearchHit preferHigherScore(SearchHit existing, SearchHit incoming) {
        return incoming.score() > existing.score() ? incoming : existing;
    }
}
