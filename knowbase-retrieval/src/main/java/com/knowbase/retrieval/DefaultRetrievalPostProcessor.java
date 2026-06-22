package com.knowbase.retrieval;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class DefaultRetrievalPostProcessor implements RetrievalPostProcessor {

    private static final int DEFAULT_MAX_CANDIDATES = 24;
    private static final int DEFAULT_RRF_K = 60;
    private static final double DEFAULT_MMR_LAMBDA = 0.72d;

    @Override
    public List<RetrievalCandidate> fuse(List<RetrievalCandidate> candidates, Map<String, Object> retrievalPolicy) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        RetrievalPolicyView policy = RetrievalPolicyView.from(retrievalPolicy);
        List<RankedCandidate> ranked = withBaseRank(candidates);
        ranked = applyContentFamilyWeights(ranked, policy);
        ranked = "rrf".equals(policy.fusion())
                ? reciprocalRankFusion(ranked, policy)
                : scoreSort(ranked);
        ranked = deduplicate(ranked, policy);
        ranked = applyLibraryBalance(ranked, policy);
        return ranked.stream()
                .limit(policy.maxCandidates())
                .map(RankedCandidate::candidate)
                .toList();
    }

    @Override
    public List<RetrievalCandidate> rerank(List<RetrievalCandidate> candidates, Map<String, Object> retrievalPolicy) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        RetrievalPolicyView policy = RetrievalPolicyView.from(retrievalPolicy);
        List<RankedCandidate> ranked = withBaseRank(candidates);
        ranked = "mmr".equals(policy.rerank())
                ? maximalMarginalRelevance(ranked, policy)
                : scoreSort(ranked);
        return ranked.stream()
                .limit(policy.maxCandidates())
                .map(RankedCandidate::candidate)
                .toList();
    }

    @Override
    public List<RetrievalCandidate> process(List<RetrievalCandidate> candidates, Map<String, Object> retrievalPolicy) {
        return rerank(fuse(candidates, retrievalPolicy), retrievalPolicy);
    }

    private static List<RankedCandidate> withBaseRank(List<RetrievalCandidate> candidates) {
        List<RankedCandidate> ranked = new ArrayList<>();
        Map<UUID, Integer> libraryRanks = new HashMap<>();
        for (int index = 0; index < candidates.size(); index++) {
            RetrievalCandidate candidate = candidates.get(index);
            int globalRank = index + 1;
            int libraryRank = libraryRanks.merge(candidate.libraryId(), 1, Integer::sum);
            ranked.add(new RankedCandidate(candidate, globalRank, libraryRank));
        }
        return ranked;
    }

    private static List<RankedCandidate> applyContentFamilyWeights(List<RankedCandidate> ranked, RetrievalPolicyView policy) {
        if (policy.contentFamilyWeights().isEmpty()) {
            return ranked;
        }
        return ranked.stream()
                .map(item -> {
                    String contentFamily = metadataText(item.candidate().metadata(), "contentFamily");
                    double weight = policy.contentFamilyWeights().getOrDefault(contentFamily.toUpperCase(Locale.ROOT), 1.0d);
                    return item.withScore(item.candidate().score() * weight);
                })
                .toList();
    }

    private static List<RankedCandidate> reciprocalRankFusion(List<RankedCandidate> ranked, RetrievalPolicyView policy) {
        return ranked.stream()
                .map(item -> {
                    double score = (1.0d / (policy.rrfK() + item.globalRank()))
                            + (1.0d / (policy.rrfK() + item.libraryRank()));
                    score += normalizedScore(item.candidate().score()) * policy.vectorScoreWeight();
                    score += readDouble(item.candidate().metadata(), "keywordScore", 0.0d) * policy.keywordScoreWeight();
                    return item.withScore(score);
                })
                .sorted(Comparator.comparingDouble((RankedCandidate item) -> item.candidate().score()).reversed())
                .toList();
    }

    private static List<RankedCandidate> scoreSort(List<RankedCandidate> ranked) {
        return ranked.stream()
                .sorted(Comparator.comparingDouble((RankedCandidate item) -> item.candidate().score()).reversed())
                .toList();
    }

    private static List<RankedCandidate> deduplicate(List<RankedCandidate> ranked, RetrievalPolicyView policy) {
        Map<String, RankedCandidate> selected = new LinkedHashMap<>();
        for (RankedCandidate item : ranked) {
            String key = policy.deduplicateByChunk()
                    ? item.candidate().libraryId() + ":" + item.candidate().chunkId()
                    : UUID.randomUUID().toString();
            if (policy.deduplicateByContent()) {
                key = key + ":" + contentFingerprint(item.candidate().content());
            }
            selected.merge(key, item, (left, right) -> left.candidate().score() >= right.candidate().score() ? left : right);
        }
        return selected.values().stream()
                .sorted(Comparator.comparingDouble((RankedCandidate item) -> item.candidate().score()).reversed())
                .toList();
    }

    private static List<RankedCandidate> applyLibraryBalance(List<RankedCandidate> ranked, RetrievalPolicyView policy) {
        if (!policy.balanceAcrossLibraries()) {
            return ranked;
        }
        int maxPerLibrary = policy.maxCandidatesPerLibrary();
        Map<UUID, Integer> counts = new HashMap<>();
        List<RankedCandidate> selected = new ArrayList<>();
        List<RankedCandidate> overflow = new ArrayList<>();
        for (RankedCandidate item : ranked) {
            int count = counts.getOrDefault(item.candidate().libraryId(), 0);
            if (count < maxPerLibrary) {
                selected.add(item);
                counts.put(item.candidate().libraryId(), count + 1);
            } else {
                overflow.add(item);
            }
        }
        if (selected.size() < policy.maxCandidates()) {
            selected.addAll(overflow.stream()
                    .limit(policy.maxCandidates() - selected.size())
                    .toList());
        }
        return selected.stream()
                .sorted(Comparator.comparingDouble((RankedCandidate item) -> item.candidate().score()).reversed())
                .toList();
    }

    private static List<RankedCandidate> maximalMarginalRelevance(List<RankedCandidate> ranked, RetrievalPolicyView policy) {
        List<RankedCandidate> remaining = new ArrayList<>(ranked);
        List<RankedCandidate> selected = new ArrayList<>();
        while (!remaining.isEmpty() && selected.size() < policy.maxCandidates()) {
            RankedCandidate best = null;
            double bestScore = Double.NEGATIVE_INFINITY;
            for (RankedCandidate candidate : remaining) {
                double diversityPenalty = selected.stream()
                        .mapToDouble(existing -> contentSimilarity(candidate.candidate().content(), existing.candidate().content()))
                        .max()
                        .orElse(0.0d);
                double mmrScore = policy.mmrLambda() * normalizedScore(candidate.candidate().score())
                        - (1.0d - policy.mmrLambda()) * diversityPenalty;
                if (best == null || mmrScore > bestScore) {
                    best = candidate;
                    bestScore = mmrScore;
                }
            }
            selected.add(best);
            remaining.remove(best);
        }
        return selected;
    }

    private static String metadataText(Map<String, Object> metadata, String key) {
        if (metadata == null || metadata.get(key) == null) {
            return "";
        }
        return String.valueOf(metadata.get(key));
    }

    private static double readDouble(Map<String, Object> metadata, String key, double defaultValue) {
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

    private static double normalizedScore(double score) {
        if (!Double.isFinite(score)) {
            return 0.0d;
        }
        return Math.max(0.0d, Math.min(1.0d, score));
    }

    private static String contentFingerprint(String content) {
        String normalized = normalizeContent(content);
        if (normalized.length() <= 180) {
            return normalized;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(normalized.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int index = 0; index < 12 && index < bytes.length; index++) {
                builder.append(String.format("%02x", bytes[index]));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            return normalized.substring(0, 180);
        }
    }

    private static String normalizeContent(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        return content.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }

    private static double contentSimilarity(String left, String right) {
        Set<String> leftTokens = tokenSet(left);
        Set<String> rightTokens = tokenSet(right);
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return 0.0d;
        }
        Set<String> intersection = new HashSet<>(leftTokens);
        intersection.retainAll(rightTokens);
        Set<String> union = new HashSet<>(leftTokens);
        union.addAll(rightTokens);
        return union.isEmpty() ? 0.0d : (double) intersection.size() / union.size();
    }

    private static Set<String> tokenSet(String text) {
        String normalized = normalizeContent(text);
        if (normalized.isBlank()) {
            return Set.of();
        }
        String[] rawTokens = normalized.split("[^\\p{IsAlphabetic}\\p{IsDigit}\\p{IsHan}]+");
        Set<String> tokens = new HashSet<>();
        for (String token : rawTokens) {
            if (token.length() >= 2) {
                tokens.add(token);
            }
        }
        if (tokens.isEmpty() && normalized.length() >= 2) {
            for (int index = 0; index < normalized.length() - 1; index++) {
                tokens.add(normalized.substring(index, index + 2));
            }
        }
        return tokens;
    }

    private record RankedCandidate(RetrievalCandidate candidate, int globalRank, int libraryRank) {

        RankedCandidate withScore(double score) {
            return new RankedCandidate(new RetrievalCandidate(
                    candidate.libraryId(),
                    candidate.documentId(),
                    candidate.chunkId(),
                    candidate.indexVersionId(),
                    candidate.content(),
                    score,
                    candidate.metadata()
            ), globalRank, libraryRank);
        }
    }

    private record RetrievalPolicyView(
            String fusion,
            String rerank,
            int maxCandidates,
            int maxCandidatesPerLibrary,
            int rrfK,
            double vectorScoreWeight,
            double keywordScoreWeight,
            double mmrLambda,
            boolean balanceAcrossLibraries,
            boolean deduplicateByChunk,
            boolean deduplicateByContent,
            Map<String, Double> contentFamilyWeights
    ) {

        static RetrievalPolicyView from(Map<String, Object> policy) {
            int maxCandidates = readInt(policy, "maxCandidates", DEFAULT_MAX_CANDIDATES);
            return new RetrievalPolicyView(
                    readString(policy, "fusion", "score").toLowerCase(Locale.ROOT),
                    readString(policy, "rerank", "none").toLowerCase(Locale.ROOT),
                    Math.max(1, maxCandidates),
                    Math.max(1, readInt(policy, "maxCandidatesPerLibrary", Math.max(1, maxCandidates / 2))),
                    Math.max(1, readInt(policy, "rrfK", DEFAULT_RRF_K)),
                    readDouble(policy, "vectorScoreWeight", 0.15d),
                    readDouble(policy, "keywordScoreWeight", 0.10d),
                    clamp(readDouble(policy, "mmrLambda", DEFAULT_MMR_LAMBDA), 0.0d, 1.0d),
                    readBoolean(policy, "balanceAcrossLibraries", false),
                    readBoolean(policy, "deduplicateByChunk", true),
                    readBoolean(policy, "deduplicateByContent", true),
                    readWeights(policy)
            );
        }

        private static String readString(Map<String, Object> policy, String key, String defaultValue) {
            if (policy == null || policy.get(key) == null) {
                return defaultValue;
            }
            String value = String.valueOf(policy.get(key));
            return value.isBlank() ? defaultValue : value;
        }

        private static int readInt(Map<String, Object> policy, String key, int defaultValue) {
            if (policy == null || policy.get(key) == null) {
                return defaultValue;
            }
            Object value = policy.get(key);
            if (value instanceof Number number) {
                return number.intValue();
            }
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException exception) {
                return defaultValue;
            }
        }

        private static double readDouble(Map<String, Object> policy, String key, double defaultValue) {
            if (policy == null || policy.get(key) == null) {
                return defaultValue;
            }
            Object value = policy.get(key);
            if (value instanceof Number number) {
                return number.doubleValue();
            }
            try {
                return Double.parseDouble(String.valueOf(value));
            } catch (NumberFormatException exception) {
                return defaultValue;
            }
        }

        private static boolean readBoolean(Map<String, Object> policy, String key, boolean defaultValue) {
            if (policy == null || policy.get(key) == null) {
                return defaultValue;
            }
            Object value = policy.get(key);
            if (value instanceof Boolean booleanValue) {
                return booleanValue;
            }
            return Boolean.parseBoolean(String.valueOf(value));
        }

        private static Map<String, Double> readWeights(Map<String, Object> policy) {
            Object raw = policy == null ? null : policy.get("contentFamilyWeights");
            if (!(raw instanceof Map<?, ?> weights)) {
                return Map.of();
            }
            Map<String, Double> result = new HashMap<>();
            weights.forEach((key, value) -> {
                if (value instanceof Number number) {
                    result.put(String.valueOf(key).toUpperCase(Locale.ROOT), number.doubleValue());
                } else {
                    try {
                        result.put(String.valueOf(key).toUpperCase(Locale.ROOT), Double.parseDouble(String.valueOf(value)));
                    } catch (NumberFormatException ignored) {
                        // 忽略非法权重，避免单个配置项让整次检索失败。
                    }
                }
            });
            return Map.copyOf(result);
        }

        private static double clamp(double value, double min, double max) {
            return Math.max(min, Math.min(max, value));
        }
    }
}
