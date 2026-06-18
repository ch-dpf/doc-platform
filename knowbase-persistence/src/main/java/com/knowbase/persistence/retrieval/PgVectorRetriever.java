package com.knowbase.persistence.retrieval;

import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.model.EmbeddingModelClient;
import com.knowbase.persistence.store.EmbeddingStore;
import com.knowbase.persistence.support.ChunkSearchRow;
import com.knowbase.retrieval.DefaultRetrievalPostProcessor;
import com.knowbase.retrieval.RetrievalPostProcessor;
import com.knowbase.retrieval.RetrievalCandidate;
import com.knowbase.retrieval.RetrievalRequest;
import com.knowbase.retrieval.Retriever;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PgVectorRetriever implements Retriever {

    private final KnowbaseRepository repository;
    private final EmbeddingModelClient embeddingModelClient;
    private final EmbeddingStore embeddingStore;
    private final RetrievalPostProcessor postProcessor;

    public PgVectorRetriever(
            KnowbaseRepository repository,
            EmbeddingModelClient embeddingModelClient,
            EmbeddingStore embeddingStore
    ) {
        this(repository, embeddingModelClient, embeddingStore, new DefaultRetrievalPostProcessor());
    }

    public PgVectorRetriever(
            KnowbaseRepository repository,
            EmbeddingModelClient embeddingModelClient,
            EmbeddingStore embeddingStore,
            RetrievalPostProcessor postProcessor
    ) {
        this.repository = repository;
        this.embeddingModelClient = embeddingModelClient;
        this.embeddingStore = embeddingStore;
        this.postProcessor = postProcessor;
    }

    @Override
    public List<RetrievalCandidate> retrieve(RetrievalRequest request) {
        float[] queryVector = embeddingModelClient.embed(List.of(request.question())).getFirst();
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
                    double keywordScore = keywordOverlap(request.question(), row.content());
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

        candidates.sort(Comparator.comparingDouble(RetrievalCandidate::score).reversed());
        return postProcessor.process(candidates, request.retrievalPolicy());
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

    private static double keywordOverlap(String question, String content) {
        if (question == null || content == null) {
            return 0.0d;
        }
        String[] tokens = question.toLowerCase().split("\\s+");
        String lowerContent = content.toLowerCase();
        int hits = 0;
        for (String token : tokens) {
            if (token.length() > 1 && lowerContent.contains(token)) {
                hits++;
            }
        }
        return tokens.length == 0 ? 0.0d : (double) hits / tokens.length;
    }
}
