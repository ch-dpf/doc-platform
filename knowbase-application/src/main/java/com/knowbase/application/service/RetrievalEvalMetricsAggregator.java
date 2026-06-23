package com.knowbase.application.service;

import com.knowbase.domain.model.RetrievalEvalSample;
import com.knowbase.retrieval.RetrievalCandidate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class RetrievalEvalMetricsAggregator {

    private RetrievalEvalMetricsAggregator() {
    }

    record SampleMetrics(
            boolean hit,
            Integer firstHitRank,
            double contextPrecisionAtK,
            String contentFamily
    ) {
    }

    static double mrr(List<SampleMetrics> samples) {
        if (samples.isEmpty()) {
            return 0.0;
        }
        double total = 0.0;
        for (SampleMetrics sample : samples) {
            if (sample.hit() && sample.firstHitRank() != null && sample.firstHitRank() > 0) {
                total += 1.0d / sample.firstHitRank();
            }
        }
        return total / samples.size();
    }

    static double averageContextPrecision(List<SampleMetrics> samples) {
        if (samples.isEmpty()) {
            return 0.0;
        }
        return samples.stream().mapToDouble(SampleMetrics::contextPrecisionAtK).average().orElse(0.0);
    }

    static Map<String, Double> stratifiedRecall(List<SampleMetrics> samples) {
        Map<String, List<Boolean>> grouped = new HashMap<>();
        for (SampleMetrics sample : samples) {
            grouped.computeIfAbsent(sample.contentFamily(), ignored -> new ArrayList<>()).add(sample.hit());
        }
        Map<String, Double> recallByFamily = new HashMap<>();
        grouped.forEach((family, hits) -> {
            long passed = hits.stream().filter(Boolean::booleanValue).count();
            recallByFamily.put(family, hits.isEmpty() ? 0.0 : (double) passed / hits.size());
        });
        return Map.copyOf(recallByFamily);
    }

    static String resolveContentFamily(
            RetrievalEvalSample sample,
            RetrievalHitEvaluator.HitEvaluation evaluation,
            List<RetrievalCandidate> candidates
    ) {
        if (evaluation.matchedChunkId() != null) {
            for (RetrievalCandidate candidate : candidates) {
                if (candidate.chunkId().equals(evaluation.matchedChunkId())) {
                    return readContentFamily(candidate);
                }
            }
        }
        if (!candidates.isEmpty()) {
            return readContentFamily(candidates.getFirst());
        }
        if (sample.notes() != null && sample.notes().contains("markdown")) {
            return "RICH_TEXT";
        }
        return "UNCLASSIFIED";
    }

    private static String readContentFamily(RetrievalCandidate candidate) {
        if (candidate.metadata() == null || candidate.metadata().get("contentFamily") == null) {
            return "UNCLASSIFIED";
        }
        return String.valueOf(candidate.metadata().get("contentFamily"));
    }
}
