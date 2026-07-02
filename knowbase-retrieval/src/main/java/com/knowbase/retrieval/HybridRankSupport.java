package com.knowbase.retrieval;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class HybridRankSupport {

    private HybridRankSupport() {
    }

    public static List<RetrievalCandidate> assignHybridRanks(List<RetrievalCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        Map<UUID, Integer> vectorRanks = rankByScore(candidates, "vectorScore", RetrievalCandidate::score);
        Map<UUID, Integer> keywordRanks = rankByScore(candidates, "keywordScore", candidate -> 0.0d);
        List<RetrievalCandidate> ranked = new ArrayList<>(candidates.size());
        for (RetrievalCandidate candidate : candidates) {
            Map<String, Object> metadata = new HashMap<>();
            if (candidate.metadata() != null) {
                metadata.putAll(candidate.metadata());
            }
            metadata.put("vectorRank", vectorRanks.getOrDefault(candidate.chunkId(), candidates.size()));
            metadata.put("keywordRank", keywordRanks.getOrDefault(candidate.chunkId(), candidates.size()));
            ranked.add(new RetrievalCandidate(
                    candidate.libraryId(),
                    candidate.documentId(),
                    candidate.chunkId(),
                    candidate.indexVersionId(),
                    candidate.content(),
                    candidate.score(),
                    Map.copyOf(metadata)
            ));
        }
        return List.copyOf(ranked);
    }

    private static Map<UUID, Integer> rankByScore(
            List<RetrievalCandidate> candidates,
            String metadataKey,
            java.util.function.ToDoubleFunction<RetrievalCandidate> fallback
    ) {
        List<RetrievalCandidate> sorted = new ArrayList<>(candidates);
        sorted.sort(Comparator.<RetrievalCandidate>comparingDouble(candidate -> readScore(candidate, metadataKey, fallback)).reversed());
        Map<UUID, Integer> ranks = new HashMap<>();
        for (int index = 0; index < sorted.size(); index++) {
            ranks.put(sorted.get(index).chunkId(), index + 1);
        }
        return ranks;
    }

    private static double readScore(
            RetrievalCandidate candidate,
            String metadataKey,
            java.util.function.ToDoubleFunction<RetrievalCandidate> fallback
    ) {
        if (candidate.metadata() != null && candidate.metadata().get(metadataKey) instanceof Number number) {
            return number.doubleValue();
        }
        return fallback.applyAsDouble(candidate);
    }
}
