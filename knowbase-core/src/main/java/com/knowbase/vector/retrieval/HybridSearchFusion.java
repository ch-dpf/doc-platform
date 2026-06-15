package com.knowbase.vector.retrieval;

import com.knowbase.vector.dto.SearchHit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 向量检索与全文检索结果的 RRF（Reciprocal Rank Fusion）融合。
 */
public final class HybridSearchFusion {

    private HybridSearchFusion() {}

    public static List<SearchHit> mergeByReciprocalRankFusion(
            List<SearchHit> vectorHits,
            List<SearchHit> keywordHits,
            int rrfK,
            int topK) {
        if (topK <= 0) {
            return List.of();
        }
        if (vectorHits == null || vectorHits.isEmpty()) {
            return trim(keywordHits, topK);
        }
        if (keywordHits == null || keywordHits.isEmpty()) {
            return trim(vectorHits, topK);
        }

        int k = Math.max(1, rrfK);
        Map<UUID, SearchHit> hitById = new HashMap<>();
        Map<UUID, Double> fusedScores = new LinkedHashMap<>();

        accumulateRrf(vectorHits, k, hitById, fusedScores);
        accumulateRrf(keywordHits, k, hitById, fusedScores);

        List<Map.Entry<UUID, Double>> ranked = new ArrayList<>(fusedScores.entrySet());
        ranked.sort(Map.Entry.<UUID, Double>comparingByValue(Comparator.reverseOrder())
                .thenComparing(entry -> entry.getKey().toString()));

        List<SearchHit> merged = new ArrayList<>(Math.min(topK, ranked.size()));
        for (Map.Entry<UUID, Double> entry : ranked) {
            if (merged.size() >= topK) {
                break;
            }
            SearchHit source = hitById.get(entry.getKey());
            if (source == null) {
                continue;
            }
            double fusedScore = entry.getValue() != null ? entry.getValue() : 0.0d;
            merged.add(new SearchHit(
                    source.chunkId(),
                    source.docId(),
                    source.tenantId(),
                    source.version(),
                    source.chunkIndex(),
                    source.content(),
                    fusedScore,
                    source.parentContext(),
                    source.chunkProfileId()));
        }
        return merged;
    }

    private static void accumulateRrf(
            List<SearchHit> hits,
            int rrfK,
            Map<UUID, SearchHit> hitById,
            Map<UUID, Double> fusedScores) {
        for (int rank = 0; rank < hits.size(); rank++) {
            SearchHit hit = hits.get(rank);
            hitById.putIfAbsent(hit.chunkId(), hit);
            double contribution = 1.0d / (rrfK + rank + 1);
            fusedScores.merge(hit.chunkId(), contribution, Double::sum);
        }
    }

    private static List<SearchHit> trim(List<SearchHit> hits, int topK) {
        if (hits == null || hits.isEmpty()) {
            return List.of();
        }
        return hits.size() <= topK ? List.copyOf(hits) : List.copyOf(hits.subList(0, topK));
    }
}
