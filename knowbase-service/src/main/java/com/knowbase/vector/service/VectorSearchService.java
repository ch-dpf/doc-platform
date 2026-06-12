package com.knowbase.vector.service;

import com.knowbase.library.config.RetrievalRulesSettings;
import com.knowbase.library.service.LibraryConfigResolver;
import com.knowbase.pipeline.config.ChunkProfileService;
import com.knowbase.vector.config.RagProperties;
import com.knowbase.vector.config.RetrievalProperties;
import com.knowbase.vector.dto.RagSearchTrace;
import com.knowbase.vector.dto.SearchHit;
import com.knowbase.vector.dto.SearchRequest;
import com.knowbase.vector.dto.SearchResponse;
import com.knowbase.vector.mapper.DocumentChunkMapper;
import com.knowbase.vector.retrieval.ChunkRerankService;
import com.knowbase.vector.retrieval.HybridSearchFusion;
import com.knowbase.vector.retrieval.MetadataFilterClause;
import com.knowbase.vector.retrieval.MetadataFilterResolver;
import com.knowbase.vector.retrieval.RetrievalHitFilter;
import com.knowbase.vector.retrieval.RetrievalMinScoreResolver;
import com.knowbase.vector.retrieval.RetrievalOrderStabilizer;
import com.knowbase.vector.rag.RagSearchQueryEnhancer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class VectorSearchService {

    private final DocumentChunkMapper chunkMapper;
    private final LibraryEmbeddingService libraryEmbeddingService;
    private final LibraryConfigResolver libraryConfigResolver;
    private final ChunkRerankService chunkRerankService;
    private final RetrievalProperties retrievalProperties;
    private final RagProperties ragProperties;
    private final ChunkProfileService chunkProfileService;

    public VectorSearchService(
            DocumentChunkMapper chunkMapper,
            LibraryEmbeddingService libraryEmbeddingService,
            LibraryConfigResolver libraryConfigResolver,
            ChunkRerankService chunkRerankService,
            RetrievalProperties retrievalProperties,
            RagProperties ragProperties,
            ChunkProfileService chunkProfileService) {
        this.chunkMapper = chunkMapper;
        this.libraryEmbeddingService = libraryEmbeddingService;
        this.libraryConfigResolver = libraryConfigResolver;
        this.chunkRerankService = chunkRerankService;
        this.retrievalProperties = retrievalProperties;
        this.ragProperties = ragProperties;
        this.chunkProfileService = chunkProfileService;
    }

    public SearchResponse search(SearchRequest request) {
        return search(request, null);
    }

    public SearchResponse search(SearchRequest request, Double minScoreOverride) {
        return new SearchResponse(runSearchPipeline(request, minScoreOverride, false, false).hits());
    }

    /** RAG 问答专用：强制向量 + BM25 混合检索，并使用关键词友好 query 做全文检索。 */
    public SearchResponse searchForRag(SearchRequest request, Double minScoreOverride) {
        return new SearchResponse(runSearchPipeline(request, minScoreOverride, true, false).hits());
    }

    /** RAG 检索预览：返回重排前候选池与最终结果，便于对比分数。 */
    public RagSearchTrace searchForRagWithTrace(SearchRequest request, Double minScoreOverride) {
        SearchPipelineResult pipeline = runSearchPipeline(request, minScoreOverride, true, true);
        return new RagSearchTrace(
                pipeline.hits(),
                pipeline.preRerankHits(),
                pipeline.rerankEnabled(),
                pipeline.rerankModel(),
                pipeline.hybridUsed());
    }

    private SearchPipelineResult runSearchPipeline(
            SearchRequest request,
            Double minScoreOverride,
            boolean forRag,
            boolean capturePreRerank) {
        RetrievalRulesSettings retrieval = libraryConfigResolver.retrievalFor(request.libraryId());
        int topK = request.topK();
        int candidateK = candidateCount(topK, retrieval, forRag);

        List<UUID> docIds = request.filter() != null && request.filter().docIds() != null
                ? request.filter().docIds()
                : Collections.emptyList();
        List<UUID> docFilter = docIds.isEmpty() ? null : docIds;
        Map<String, String> metadataRequest = request.filter() != null ? request.filter().metadata() : null;
        List<MetadataFilterClause> metadataFilters =
                MetadataFilterResolver.resolve(metadataRequest, retrieval);
        List<String> chunkProfileIds = chunkProfileService.resolveRetrievalProfileIds(
                request.libraryId(),
                Boolean.TRUE.equals(request.includeAllChunkProfiles()),
                request.chunkProfileIds());

        float[] queryVector = libraryEmbeddingService.embed(request.libraryId(), request.query());
        List<SearchHit> vectorHits = chunkMapper.search(
                request.libraryId(),
                request.tenantId(),
                queryVector,
                candidateK,
                docFilter,
                metadataFilters,
                chunkProfileIds);
        Map<UUID, Double> vectorScoresByChunkId = toScoreMap(vectorHits);

        List<SearchHit> hits;
        boolean useHybrid = (retrieval.isHybridSearchEnabled() || forRag) && isKeywordQuery(request.query());
        if (useHybrid) {
            String keywordQuery = forRag
                    ? RagSearchQueryEnhancer.toKeywordQuery(request.query())
                    : request.query().trim();
            if (keywordQuery.isBlank()) {
                keywordQuery = request.query().trim();
            }
            List<SearchHit> keywordHits = chunkMapper.keywordSearch(
                    request.libraryId(),
                    request.tenantId(),
                    keywordQuery,
                    candidateK,
                    docFilter,
                    metadataFilters,
                    chunkProfileIds);
            hits = HybridSearchFusion.mergeByReciprocalRankFusion(
                    vectorHits,
                    keywordHits,
                    retrievalProperties.getRrfK(),
                    candidateK);
        } else {
            hits = vectorHits;
        }

        boolean rerankEnabled = retrieval.isRerankEnabled();
        String rerankModel = retrieval.getRerankModel() != null ? retrieval.getRerankModel().trim() : "";
        int outputK = topK;
        if (forRag && rerankEnabled) {
            outputK = Math.min(candidateK, Math.max(topK * 2, topK));
        }

        List<SearchHit> preRerankHits = List.of();
        if (capturePreRerank && !hits.isEmpty()) {
            int preRerankLimit = rerankEnabled
                    ? Math.min(hits.size(), retrievalProperties.getMaxRerankCandidates())
                    : Math.min(hits.size(), outputK);
            preRerankHits = List.copyOf(hits.subList(0, preRerankLimit));
        }

        if (rerankEnabled) {
            hits = chunkRerankService.rerank(
                    request.libraryId(),
                    request.query(),
                    hits,
                    retrieval,
                    outputK);
        } else if (hits.size() > outputK) {
            hits = List.copyOf(hits.subList(0, outputK));
        }

        double minScore = RetrievalMinScoreResolver.resolve(minScoreOverride, retrieval, ragProperties);
        List<SearchHit> filtered = filterByMinScore(hits, minScore, vectorScoresByChunkId, rerankEnabled);
        if (forRag) {
            filtered = RetrievalHitFilter.preferContentChunks(filtered, topK);
        } else if (filtered.size() > topK) {
            filtered = List.copyOf(filtered.subList(0, topK));
        }
        return new SearchPipelineResult(
                RetrievalOrderStabilizer.stabilize(filtered),
                preRerankHits,
                rerankEnabled,
                rerankModel,
                useHybrid);
    }

    private record SearchPipelineResult(
            List<SearchHit> hits,
            List<SearchHit> preRerankHits,
            boolean rerankEnabled,
            String rerankModel,
            boolean hybridUsed) {
    }

    private int candidateCount(int topK, RetrievalRulesSettings retrieval, boolean forRag) {
        int multiplier = Math.max(1, retrievalProperties.getCandidateMultiplier());
        int desired = topK * multiplier;
        if (forRag || retrieval.isHybridSearchEnabled() || retrieval.isRerankEnabled()) {
            desired = Math.max(desired, topK * 2);
        }
        return Math.min(Math.max(desired, topK), retrievalProperties.getMaxCandidates());
    }

    private static boolean isKeywordQuery(String query) {
        return query != null && !query.isBlank();
    }

    private static Map<UUID, Double> toScoreMap(List<SearchHit> hits) {
        Map<UUID, Double> scores = new HashMap<>();
        if (hits == null) {
            return scores;
        }
        for (SearchHit hit : hits) {
            scores.putIfAbsent(hit.chunkId(), hit.score());
        }
        return scores;
    }

    private static List<SearchHit> filterByMinScore(
            List<SearchHit> hits,
            double minScore,
            Map<UUID, Double> vectorScoresByChunkId,
            boolean rerankEnabled) {
        if (minScore <= 0 || hits == null || hits.isEmpty()) {
            return hits == null ? List.of() : hits;
        }
        List<SearchHit> filtered = new ArrayList<>();
        for (SearchHit hit : hits) {
            if (rerankEnabled) {
                if (hit.score() >= minScore) {
                    filtered.add(hit);
                }
                continue;
            }
            Double vectorScore = vectorScoresByChunkId.get(hit.chunkId());
            if (vectorScore == null || vectorScore >= minScore) {
                filtered.add(hit);
            }
        }
        return filtered;
    }
}
