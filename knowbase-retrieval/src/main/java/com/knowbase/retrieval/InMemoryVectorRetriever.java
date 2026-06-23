package com.knowbase.retrieval;

import com.knowbase.domain.model.IndexedChunk;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.model.EmbeddingModelClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class InMemoryVectorRetriever implements Retriever {

    private final KnowbaseRepository repository;
    private final EmbeddingModelClient embeddingModelClient;

    public InMemoryVectorRetriever(KnowbaseRepository repository, EmbeddingModelClient embeddingModelClient) {
        this.repository = repository;
        this.embeddingModelClient = embeddingModelClient;
    }

    @Override
    public List<RetrievalCandidate> retrieve(RetrievalRequest request) {
        String queryText = resolveQueryText(request);
        Map<String, Object> policy = request.retrievalPolicy();
        String retrievalMode = RetrievalModes.resolve(policy);
        int topKPerLibrary = readInt(policy, "topKPerLibrary", 8);
        List<RetrievalCandidate> candidates = new ArrayList<>();

        for (UUID libraryId : request.libraryIds()) {
            LibraryProfile profile = repository.findLatestLibraryProfile(libraryId).orElse(null);
            int topK = profile == null ? topKPerLibrary : profile.retrievalTopK();
            repository.findPublishedIndexVersion(libraryId).ifPresent(indexVersion -> {
                List<IndexedChunk> chunks = repository.listChunksByIndexVersion(indexVersion.indexVersionId());
                float[] queryVector = needsEmbedding(retrievalMode)
                        ? embeddingModelClient.embed(List.of(queryText)).getFirst()
                        : null;
                List<RetrievalCandidate> libraryCandidates = new ArrayList<>();
                for (IndexedChunk indexedChunk : chunks) {
                    if (!ChunkRetrievalSupport.isRetrievalEnabled(indexedChunk.chunk().metadata())) {
                        continue;
                    }
                    RetrievalCandidate candidate = scoreChunk(
                            indexedChunk,
                            queryText,
                            queryVector,
                            retrievalMode,
                            policy,
                            "in_memory"
                    );
                    if (candidate != null) {
                        libraryCandidates.add(candidate);
                    }
                }
                libraryCandidates.sort(Comparator.comparingDouble(RetrievalCandidate::score).reversed());
                List<RetrievalCandidate> ranked = HybridRankSupport.assignHybridRanks(
                        libraryCandidates.stream().limit(topK).toList()
                );
                candidates.addAll(ranked);
            });
        }

        return candidates;
    }

    private static RetrievalCandidate scoreChunk(
            IndexedChunk indexedChunk,
            String queryText,
            float[] queryVector,
            String retrievalMode,
            Map<String, Object> policy,
            String backend
    ) {
        if (RetrievalModes.VECTOR.equals(retrievalMode) && indexedChunk.embedding() == null) {
            return null;
        }
        double vectorScore = 0.0d;
        if (queryVector != null && indexedChunk.embedding() != null) {
            vectorScore = cosineSimilarity(queryVector, indexedChunk.embedding());
        }
        double keywordScore = KeywordScorer.overlap(queryText, indexedChunk.chunk().content(), policy);
        if (RetrievalModes.KEYWORD.equals(retrievalMode) && keywordScore <= 0.0d) {
            return null;
        }
        if (RetrievalModes.VECTOR.equals(retrievalMode) && vectorScore <= 0.0d) {
            return null;
        }
        double score = RetrievalScoreComposer.finalScore(retrievalMode, vectorScore, keywordScore, policy);
        Map<String, Object> metadata = indexedChunk.chunk().metadata() == null ? Map.of() : indexedChunk.chunk().metadata();
        metadata = RetrievalMetadata.enrich(metadata, vectorScore, keywordScore, retrievalMode);
        metadata = withBackend(metadata, backend);
        return new RetrievalCandidate(
                indexedChunk.chunk().libraryId(),
                indexedChunk.chunk().documentId(),
                indexedChunk.chunk().chunkId(),
                indexedChunk.chunk().indexVersionId(),
                indexedChunk.chunk().content(),
                score,
                metadata
        );
    }

    private static boolean needsEmbedding(String retrievalMode) {
        return RetrievalModes.VECTOR.equals(retrievalMode) || RetrievalModes.HYBRID.equals(retrievalMode);
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

    private static Map<String, Object> withBackend(Map<String, Object> metadata, String backend) {
        Map<String, Object> enriched = new java.util.HashMap<>(metadata);
        enriched.put("retrievalBackend", backend);
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
}
