package com.knowbase.persistence.retrieval;

import com.knowbase.domain.model.IndexedChunk;
import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.model.EmbeddingModelClient;
import com.knowbase.persistence.store.EmbeddingStore;
import com.knowbase.persistence.support.ChunkSearchRow;
import com.knowbase.retrieval.ChunkRetrievalSupport;
import com.knowbase.retrieval.HybridRankSupport;
import com.knowbase.retrieval.KeywordScorer;
import com.knowbase.retrieval.RetrievalCandidate;
import com.knowbase.retrieval.RetrievalCandidateMerger;
import com.knowbase.retrieval.RetrievalMetadata;
import com.knowbase.retrieval.RetrievalModes;
import com.knowbase.retrieval.RetrievalRequest;
import com.knowbase.retrieval.RetrievalScoreComposer;
import com.knowbase.retrieval.Retriever;

import java.util.ArrayList;
import java.util.Comparator;
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
        Map<String, Object> policy = request.retrievalPolicy();
        String retrievalMode = RetrievalModes.resolve(policy);
        int topKPerLibrary = readInt(policy, "topKPerLibrary", 8);
        List<RetrievalCandidate> candidates = new ArrayList<>();

        for (UUID libraryId : request.libraryIds()) {
            int topK = repository.findLatestLibraryProfile(libraryId)
                    .map(profile -> profile.retrievalTopK())
                    .orElse(topKPerLibrary);
            repository.findPublishedIndexVersion(libraryId).ifPresent(indexVersion -> {
                UUID indexVersionId = indexVersion.indexVersionId();
                List<RetrievalCandidate> libraryCandidates = switch (retrievalMode) {
                    case RetrievalModes.VECTOR -> retrieveVectorOnly(queryText, indexVersionId, topK, retrievalMode, policy);
                    case RetrievalModes.KEYWORD -> retrieveKeywordOnly(queryText, indexVersionId, topK, retrievalMode, policy);
                    default -> retrieveHybrid(queryText, indexVersionId, topK, policy);
                };
                candidates.addAll(HybridRankSupport.assignHybridRanks(libraryCandidates));
            });
        }

        return candidates;
    }

    private List<RetrievalCandidate> retrieveVectorOnly(
            String queryText,
            UUID indexVersionId,
            int topK,
            String retrievalMode,
            Map<String, Object> policy
    ) {
        float[] queryVector = embeddingModelClient.embed(List.of(queryText)).getFirst();
        List<ChunkSearchRow> rows = embeddingStore.searchSimilar(indexVersionId, queryVector, topK);
        List<RetrievalCandidate> candidates = new ArrayList<>(rows.size());
        for (ChunkSearchRow row : rows) {
            if (!ChunkRetrievalSupport.isRetrievalEnabled(row.metadata())) {
                continue;
            }
            double vectorScore = row.score();
            double keywordScore = KeywordScorer.overlap(queryText, row.content(), policy);
            double score = RetrievalScoreComposer.finalScore(retrievalMode, vectorScore, keywordScore, policy);
            candidates.add(toCandidate(row, score, vectorScore, keywordScore, retrievalMode));
        }
        return candidates;
    }

    private List<RetrievalCandidate> retrieveKeywordOnly(
            String queryText,
            UUID indexVersionId,
            int topK,
            String retrievalMode,
            Map<String, Object> policy
    ) {
        List<RetrievalCandidate> scored = new ArrayList<>();
        for (IndexedChunk indexedChunk : repository.listChunksByIndexVersion(indexVersionId)) {
            if (!ChunkRetrievalSupport.isRetrievalEnabled(indexedChunk.chunk().metadata())) {
                continue;
            }
            double keywordScore = KeywordScorer.overlap(queryText, indexedChunk.chunk().content(), policy);
            if (keywordScore <= 0.0d) {
                continue;
            }
            scored.add(new RetrievalCandidate(
                    indexedChunk.chunk().libraryId(),
                    indexedChunk.chunk().documentId(),
                    indexedChunk.chunk().chunkId(),
                    indexedChunk.chunk().indexVersionId(),
                    indexedChunk.chunk().content(),
                    keywordScore,
                    buildMetadata(indexedChunk.chunk().metadata(), 0.0d, keywordScore, retrievalMode)
            ));
        }
        scored.sort(Comparator.comparingDouble(RetrievalCandidate::score).reversed());
        return scored.stream().limit(topK).toList();
    }

    private List<RetrievalCandidate> retrieveHybrid(
            String queryText,
            UUID indexVersionId,
            int topK,
            Map<String, Object> policy
    ) {
        float[] queryVector = embeddingModelClient.embed(List.of(queryText)).getFirst();
        List<RetrievalCandidate> vectorCandidates = new ArrayList<>();
        for (ChunkSearchRow row : embeddingStore.searchSimilar(indexVersionId, queryVector, topK)) {
            if (!ChunkRetrievalSupport.isRetrievalEnabled(row.metadata())) {
                continue;
            }
            double vectorScore = row.score();
            double keywordScore = KeywordScorer.overlap(queryText, row.content(), policy);
            double score = RetrievalScoreComposer.finalScore(RetrievalModes.HYBRID, vectorScore, keywordScore, policy);
            vectorCandidates.add(toCandidate(row, score, vectorScore, keywordScore, RetrievalModes.HYBRID));
        }
        List<RetrievalCandidate> keywordCandidates = retrieveKeywordOnly(
                queryText,
                indexVersionId,
                topK,
                RetrievalModes.KEYWORD,
                policy
        );
        List<RetrievalCandidate> merged = RetrievalCandidateMerger.mergeHybridPools(vectorCandidates, keywordCandidates, policy);
        return merged.stream().limit(topK).toList();
    }

    private static RetrievalCandidate toCandidate(
            ChunkSearchRow row,
            double score,
            double vectorScore,
            double keywordScore,
            String retrievalMode
    ) {
        return new RetrievalCandidate(
                row.libraryId(),
                row.documentId(),
                row.chunkId(),
                row.indexVersionId(),
                row.content(),
                score,
                buildMetadata(row.metadata(), vectorScore, keywordScore, retrievalMode)
        );
    }

    private static Map<String, Object> buildMetadata(
            Map<String, Object> metadata,
            double vectorScore,
            double keywordScore,
            String retrievalMode
    ) {
        Map<String, Object> enriched = RetrievalMetadata.enrich(metadata, vectorScore, keywordScore, retrievalMode);
        Map<String, Object> withBackend = new java.util.HashMap<>(enriched);
        withBackend.put("retrievalBackend", "pgvector");
        return Map.copyOf(withBackend);
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
}
