package com.knowbase.persistence.retrieval;

import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.model.EmbeddingModelClient;
import com.knowbase.persistence.store.EmbeddingStore;
import com.knowbase.persistence.support.ChunkSearchRow;
import com.knowbase.retrieval.RetrievalCandidate;
import com.knowbase.retrieval.RetrievalRequest;
import com.knowbase.retrieval.Retriever;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PgVectorRetriever implements Retriever {

    private final KnowbaseRepository repository;
    private final EmbeddingModelClient embeddingModelClient;
    private final EmbeddingStore embeddingStore;

    public PgVectorRetriever(
            KnowbaseRepository repository,
            EmbeddingModelClient embeddingModelClient,
            EmbeddingStore embeddingStore
    ) {
        this.repository = repository;
        this.embeddingModelClient = embeddingModelClient;
        this.embeddingStore = embeddingStore;
    }

    @Override
    public List<RetrievalCandidate> retrieve(RetrievalRequest request) {
        String queryText = resolveQueryText(request);
        float[] queryVector = embeddingModelClient.embed(List.of(queryText)).getFirst();
        int topKPerLibrary = readInt(request.retrievalPolicy(), "topKPerLibrary", 8);
        List<RetrievalCandidate> candidates = new ArrayList<>();

        for (UUID libraryId : request.libraryIds()) {
            int topK = repository.findLatestLibraryProfile(libraryId)
                    .map(profile -> profile.retrievalTopK())
                    .orElse(topKPerLibrary);
            repository.findPublishedIndexVersion(libraryId).ifPresent(indexVersion -> {
                List<ChunkSearchRow> rows = embeddingStore.searchSimilar(indexVersion.indexVersionId(), queryVector, topK);
                for (ChunkSearchRow row : rows) {
                    double vectorScore = row.score();
                    double keywordScore = keywordOverlap(queryText, row.content(), request.retrievalPolicy());
                    double score = vectorScore + keywordScore * 0.2d;
                    candidates.add(new RetrievalCandidate(
                            row.libraryId(),
                            row.documentId(),
                            row.chunkId(),
                            row.indexVersionId(),
                            row.content(),
                            score,
                            enrichRetrievalMetadata(row.metadata(), vectorScore, keywordScore)
                    ));
                }
            });
        }

        return candidates;
    }

    private static String resolveQueryText(RetrievalRequest request) {
        if (request.question() != null && !request.question().isBlank()) {
            return request.question();
        }
        Object expanded = request.retrievalPolicy() == null ? null : request.retrievalPolicy().get("expandedQueries");
        if (expanded instanceof List<?> queries && !queries.isEmpty()) {
            return String.valueOf(queries.getFirst());
        }
        return "";
    }

    private static int readInt(Map<String, Object> policy, String key, int defaultValue) {
        if (policy == null || policy.get(key) == null) {
            return defaultValue;
        }
        Object value = policy.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return defaultValue;
    }

    private static Map<String, Object> enrichRetrievalMetadata(
            Map<String, Object> metadata,
            double vectorScore,
            double keywordScore
    ) {
        Map<String, Object> enriched = new HashMap<>();
        if (metadata != null) {
            enriched.putAll(metadata);
        }
        enriched.put("vectorScore", vectorScore);
        enriched.put("keywordScore", keywordScore);
        enriched.put("retrievalBackend", "pgvector");
        return Map.copyOf(enriched);
    }

    private static double keywordOverlap(String question, String content, Map<String, Object> policy) {
        if (question == null || content == null) {
            return 0.0d;
        }
        String lowerContent = content.toLowerCase();
        List<String> tokens = new ArrayList<>();
        String[] questionTokens = question.toLowerCase().split("\\s+");
        for (String token : questionTokens) {
            if (token.length() > 1) {
                tokens.add(token);
            }
        }
        Object keywords = policy == null ? null : policy.get("queryKeywords");
        if (keywords instanceof List<?> keywordList) {
            for (Object keyword : keywordList) {
                String value = String.valueOf(keyword);
                if (value.length() > 1) {
                    tokens.add(value.toLowerCase());
                }
            }
        }
        if (tokens.isEmpty()) {
            return 0.0d;
        }
        int hits = 0;
        for (String token : tokens) {
            if (lowerContent.contains(token)) {
                hits++;
            }
        }
        return (double) hits / tokens.size();
    }
}
