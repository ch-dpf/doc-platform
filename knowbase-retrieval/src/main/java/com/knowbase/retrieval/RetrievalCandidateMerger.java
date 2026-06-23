package com.knowbase.retrieval;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class RetrievalCandidateMerger {

    private RetrievalCandidateMerger() {
    }

    public static List<RetrievalCandidate> mergeHybridPools(
            List<RetrievalCandidate> vectorCandidates,
            List<RetrievalCandidate> keywordCandidates,
            Map<String, Object> policy
    ) {
        Map<UUID, ScorePair> merged = new HashMap<>();
        for (RetrievalCandidate candidate : vectorCandidates) {
            merged.put(candidate.chunkId(), ScorePair.from(candidate, true));
        }
        for (RetrievalCandidate candidate : keywordCandidates) {
            merged.merge(candidate.chunkId(), ScorePair.from(candidate, false), ScorePair::merge);
        }
        List<RetrievalCandidate> combined = new ArrayList<>(merged.size());
        for (ScorePair pair : merged.values()) {
            combined.add(pair.toCandidate(policy));
        }
        combined.sort(Comparator.comparingDouble(RetrievalCandidate::score).reversed());
        return combined;
    }

    private record ScorePair(
            UUID libraryId,
            UUID documentId,
            UUID chunkId,
            UUID indexVersionId,
            String content,
            double vectorScore,
            double keywordScore,
            Map<String, Object> metadata
    ) {

        static ScorePair from(RetrievalCandidate candidate, boolean vectorPool) {
            double vectorScore = readMetadataDouble(candidate.metadata(), "vectorScore", vectorPool ? candidate.score() : 0.0d);
            double keywordScore = readMetadataDouble(candidate.metadata(), "keywordScore", vectorPool ? 0.0d : candidate.score());
            if (vectorPool && keywordScore == 0.0d) {
                keywordScore = readMetadataDouble(candidate.metadata(), "keywordScore", 0.0d);
            }
            if (!vectorPool && vectorScore == 0.0d) {
                vectorScore = readMetadataDouble(candidate.metadata(), "vectorScore", 0.0d);
            }
            Map<String, Object> metadata = candidate.metadata() == null ? Map.of() : candidate.metadata();
            return new ScorePair(
                    candidate.libraryId(),
                    candidate.documentId(),
                    candidate.chunkId(),
                    candidate.indexVersionId(),
                    candidate.content(),
                    vectorScore,
                    keywordScore,
                    metadata
            );
        }

        ScorePair merge(ScorePair other) {
            return new ScorePair(
                    libraryId,
                    documentId,
                    chunkId,
                    indexVersionId,
                    content == null || content.isBlank() ? other.content : content,
                    Math.max(vectorScore, other.vectorScore),
                    Math.max(keywordScore, other.keywordScore),
                    metadata.isEmpty() ? other.metadata : metadata
            );
        }

        RetrievalCandidate toCandidate(Map<String, Object> policy) {
            String mode = RetrievalModes.HYBRID;
            double score = RetrievalScoreComposer.finalScore(mode, vectorScore, keywordScore, policy);
            Map<String, Object> enriched = RetrievalMetadata.enrich(metadata, vectorScore, keywordScore, mode);
            return new RetrievalCandidate(libraryId, documentId, chunkId, indexVersionId, content, score, enriched);
        }

        private static double readMetadataDouble(Map<String, Object> metadata, String key, double defaultValue) {
            if (metadata == null || metadata.get(key) == null) {
                return defaultValue;
            }
            Object value = metadata.get(key);
            if (value instanceof Number number) {
                return number.doubleValue();
            }
            try {
                return Double.parseDouble(String.valueOf(value));
            } catch (NumberFormatException exception) {
                return defaultValue;
            }
        }
    }
}
