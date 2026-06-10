package com.knowbase.vector.retrieval;

import com.knowbase.vector.dto.SearchHit;

import java.util.Comparator;
import java.util.List;

/** 对检索结果做确定性排序，避免分数接近时每次 Top-K 顺序抖动。 */
public final class RetrievalOrderStabilizer {

    private RetrievalOrderStabilizer() {}

    public static List<SearchHit> stabilize(List<SearchHit> hits) {
        if (hits == null || hits.size() <= 1) {
            return hits == null ? List.of() : hits;
        }
        return hits.stream()
                .sorted(Comparator.comparingDouble(SearchHit::score).reversed()
                        .thenComparing(SearchHit::docId)
                        .thenComparingInt(SearchHit::chunkIndex)
                        .thenComparing(SearchHit::chunkId))
                .toList();
    }
}
