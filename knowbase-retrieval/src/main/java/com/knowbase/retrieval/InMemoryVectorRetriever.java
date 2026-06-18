package com.knowbase.retrieval;

import com.knowbase.domain.model.IndexedChunk;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.model.EmbeddingModelClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class InMemoryVectorRetriever implements Retriever {

    private final KnowbaseRepository repository;
    private final EmbeddingModelClient embeddingModelClient;
    private final RetrievalPostProcessor postProcessor;

    public InMemoryVectorRetriever(KnowbaseRepository repository, EmbeddingModelClient embeddingModelClient) {
        this(repository, embeddingModelClient, new DefaultRetrievalPostProcessor());
    }

    public InMemoryVectorRetriever(
            KnowbaseRepository repository,
            EmbeddingModelClient embeddingModelClient,
            RetrievalPostProcessor postProcessor
    ) {
        this.repository = repository;
        this.embeddingModelClient = embeddingModelClient;
        this.postProcessor = postProcessor;
    }

    @Override
    public List<RetrievalCandidate> retrieve(RetrievalRequest request) {
        float[] queryVector = embeddingModelClient.embed(List.of(request.question())).getFirst();
        int topKPerLibrary = readInt(request.retrievalPolicy(), "topKPerLibrary", 8);
        List<RetrievalCandidate> candidates = new ArrayList<>();

        for (UUID libraryId : request.libraryIds()) {
            LibraryProfile profile = repository.findLatestLibraryProfile(libraryId).orElse(null);
            int topK = profile == null ? topKPerLibrary : profile.retrievalTopK();
            repository.findPublishedIndexVersion(libraryId).ifPresent(indexVersion -> {
                List<IndexedChunk> chunks = repository.listChunksByIndexVersion(indexVersion.indexVersionId());
                List<RetrievalCandidate> libraryCandidates = new ArrayList<>();
                for (IndexedChunk indexedChunk : chunks) {
                    double vectorScore = cosineSimilarity(queryVector, indexedChunk.embedding());
                    double keywordScore = keywordOverlap(request.question(), indexedChunk.chunk().content());
                    double score = vectorScore + keywordScore * 0.2d;
                    libraryCandidates.add(new RetrievalCandidate(
                            indexedChunk.chunk().libraryId(),
                            indexedChunk.chunk().documentId(),
                            indexedChunk.chunk().chunkId(),
                            indexedChunk.chunk().indexVersionId(),
                            indexedChunk.chunk().content(),
                            score,
                            enrichRetrievalMetadata(indexedChunk.chunk().metadata(), vectorScore, keywordScore)
                    ));
                }
                libraryCandidates.sort(Comparator.comparingDouble(RetrievalCandidate::score).reversed());
                candidates.addAll(libraryCandidates.stream().limit(topK).toList());
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
        enriched.put("retrievalBackend", "in_memory");
        return Map.copyOf(enriched);
    }

    private static double cosineSimilarity(float[] left, float[] right) {
        int length = Math.min(left.length, right.length);
        double dot = 0.0d;
        double leftNorm = 0.0d;
        double rightNorm = 0.0d;
        for (int index = 0; index < length; index++) {
            dot += left[index] * right[index];
            leftNorm += left[index] * left[index];
            rightNorm += right[index] * right[index];
        }
        if (leftNorm == 0.0d || rightNorm == 0.0d) {
            return 0.0d;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
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
