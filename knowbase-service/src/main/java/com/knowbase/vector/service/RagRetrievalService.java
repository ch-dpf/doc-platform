package com.knowbase.vector.service;



import com.knowbase.ingest.support.DocMetadataStore;
import com.knowbase.pipeline.config.ChunkProfileService;
import com.knowbase.library.service.LibraryConfigResolver;
import com.knowbase.vector.retrieval.RetrievalTopKResolver;

import com.knowbase.vector.config.RagProperties;

import com.knowbase.vector.dto.RagChatMessage;

import com.knowbase.vector.dto.RagChatRequest;

import com.knowbase.vector.dto.RagRetrievalTrace;
import com.knowbase.vector.dto.RagRetrievalPreviewHit;

import com.knowbase.vector.dto.RagRetrievalPreviewRequest;

import com.knowbase.vector.dto.RagRetrievalPreviewResponse;

import com.knowbase.vector.dto.RagRetrievalTrace;

import com.knowbase.vector.dto.RagSearchTrace;

import com.knowbase.vector.dto.SearchHit;

import com.knowbase.vector.dto.SearchRequest;

import com.knowbase.vector.rag.RagConversationSupport;

import com.knowbase.vector.rag.RagHitMerger;

import com.knowbase.vector.rag.RagPromptBuilder;

import com.knowbase.vector.rag.RagQuestionAnalyzer;

import com.knowbase.vector.rag.RagQueryRewriteService;

import com.knowbase.vector.rag.RagSearchQueryEnhancer;

import com.knowbase.vector.rag.WeeklyReportWorkItemExtractor;

import com.knowbase.vector.retrieval.RagRetrievalCache;

import com.knowbase.vector.retrieval.RetrievalHitFilter;

import org.springframework.stereotype.Service;



import java.util.ArrayList;

import java.util.List;

import java.util.Map;

import java.util.UUID;

import java.util.stream.Collectors;



@Service

public class RagRetrievalService {



    private final VectorSearchService searchService;

    private final RagRetrievalCache retrievalCache;

    private final RagProperties ragProperties;

    private final RagPromptBuilder promptBuilder;

    private final DocMetadataStore docMetadataStore;

    private final RagQueryRewriteService queryRewriteService;

    private final LibraryConfigResolver libraryConfigResolver;
    private final ChunkProfileService chunkProfileService;

    public RagRetrievalService(

            VectorSearchService searchService,

            RagRetrievalCache retrievalCache,

            RagProperties ragProperties,

            RagPromptBuilder promptBuilder,

            DocMetadataStore docMetadataStore,

            RagQueryRewriteService queryRewriteService,

            LibraryConfigResolver libraryConfigResolver,
            ChunkProfileService chunkProfileService) {

        this.searchService = searchService;

        this.retrievalCache = retrievalCache;

        this.ragProperties = ragProperties;

        this.promptBuilder = promptBuilder;

        this.docMetadataStore = docMetadataStore;

        this.queryRewriteService = queryRewriteService;

        this.libraryConfigResolver = libraryConfigResolver;
        this.chunkProfileService = chunkProfileService;
    }



    public record RetrievalResult(
            List<SearchHit> hits,
            String conversationQuery,
            String searchQuery,
            String keywordQuery,
            int effectiveTopK,
            boolean cacheHit,
            List<SearchHit> preRerankHits,
            boolean rerankEnabled,
            String rerankModel,
            boolean hybridUsed) {}

    public RagRetrievalTrace buildTrace(UUID libraryId, RetrievalResult result) {
        List<SearchHit> allHits = new ArrayList<>(result.hits());
        if (result.preRerankHits() != null) {
            for (SearchHit hit : result.preRerankHits()) {
                if (allHits.stream().noneMatch(h -> h.chunkId().equals(hit.chunkId()))) {
                    allHits.add(hit);
                }
            }
        }
        Map<UUID, String> fileNames = resolveFileNames(allHits);
        String preRerankScoreLabel = result.hybridUsed() ? "RRF 融合分" : "向量相似度";
        String finalScoreLabel = result.rerankEnabled() ? "重排余弦分" : preRerankScoreLabel;
        List<RagRetrievalPreviewHit> previewHits = toPreviewHits(libraryId, result.hits(), fileNames);
        List<RagRetrievalPreviewHit> preRerankHits = toPreviewHits(
                libraryId,
                result.preRerankHits() != null ? result.preRerankHits() : List.of(),
                fileNames);
        return new RagRetrievalTrace(
                result.conversationQuery(),
                result.searchQuery(),
                result.keywordQuery(),
                result.effectiveTopK(),
                result.cacheHit(),
                result.rerankEnabled(),
                result.rerankModel(),
                preRerankScoreLabel,
                finalScoreLabel,
                previewHits.size(),
                preRerankHits.size(),
                previewHits,
                preRerankHits,
                null);
    }



    public RetrievalResult retrieve(RagChatRequest request, int topK) {

        int effectiveTopK = resolveEffectiveTopK(request.question(), topK);

        List<RagChatMessage> history = RagConversationSupport.sanitizeHistory(

                request.history(),

                ragProperties.getMaxHistoryMessages(),

                ragProperties.getMaxHistoryMessageChars());

        String conversationQuery = RagConversationSupport.resolveSearchQuery(request.question(), history);

        String searchQuery = queryRewriteService.rewrite(conversationQuery, request.question(), history);

        String keywordQuery = resolveKeywordQuery(request.question(), searchQuery, conversationQuery);



        var cached = retrievalCache.get(

                request.libraryId(),

                request.tenantId(),

                searchQuery,

                keywordQuery,

                effectiveTopK,

                request.minScore(),

                request.filter());

        if (cached != null) {
            return new RetrievalResult(
                    cached.hits(),
                    conversationQuery,
                    searchQuery,
                    keywordQuery,
                    effectiveTopK,
                    true,
                    List.of(),
                    false,
                    null,
                    false);
        }

        PreviewTraceResult traceResult = retrieveUncachedWithTrace(
                request, searchQuery, keywordQuery, effectiveTopK);
        retrievalCache.put(
                request.libraryId(),
                request.tenantId(),
                searchQuery,
                keywordQuery,
                effectiveTopK,
                request.minScore(),
                request.filter(),
                traceResult.hits());
        return new RetrievalResult(
                traceResult.hits(),
                conversationQuery,
                searchQuery,
                keywordQuery,
                effectiveTopK,
                false,
                traceResult.preRerankHits(),
                traceResult.rerankEnabled(),
                traceResult.rerankModel(),
                traceResult.hybridUsed());

    }



    public RagRetrievalPreviewResponse preview(RagRetrievalPreviewRequest request) {

        RagChatRequest ragRequest = new RagChatRequest(
                request.libraryId(),
                request.tenantId(),
                request.question(),
                request.topK(),
                request.minScore(),
                request.filter(),
                null,
                request.history(),
                request.includeAllChunkProfiles(),
                request.chunkProfileIds());

        int topK = RetrievalTopKResolver.resolve(
                request.topK(),
                libraryConfigResolver.retrievalFor(request.libraryId()),
                ragProperties);

        int effectiveTopK = resolveEffectiveTopK(request.question(), topK);

        List<RagChatMessage> history = RagConversationSupport.sanitizeHistory(

                request.history(),

                ragProperties.getMaxHistoryMessages(),

                ragProperties.getMaxHistoryMessageChars());

        String conversationQuery = RagConversationSupport.resolveSearchQuery(request.question(), history);

        if (RagQuestionAnalyzer.isCalendarYearQuestion(request.question())) {
            return calendarYearPreviewResponse(request.question(), conversationQuery, effectiveTopK);
        }

        String searchQuery = queryRewriteService.rewrite(conversationQuery, request.question(), history);

        String keywordQuery = resolveKeywordQuery(request.question(), searchQuery, conversationQuery);

        PreviewTraceResult traceResult = retrieveUncachedWithTrace(

                ragRequest, searchQuery, keywordQuery, effectiveTopK);

        Map<UUID, String> fileNames = resolveFileNames(traceResult.allHits());

        String preRerankScoreLabel = traceResult.hybridUsed() ? "RRF 融合分" : "向量相似度";

        String finalScoreLabel = traceResult.rerankEnabled() ? "重排余弦分" : preRerankScoreLabel;

        List<RagRetrievalPreviewHit> preRerankHits =
                toPreviewHits(request.libraryId(), traceResult.preRerankHits(), fileNames);

        List<RagRetrievalPreviewHit> previewHits =
                toPreviewHits(request.libraryId(), traceResult.hits(), fileNames);

        return new RagRetrievalPreviewResponse(

                request.question().strip(),

                conversationQuery,

                searchQuery,

                keywordQuery,

                effectiveTopK,

                false,

                previewHits.size(),

                traceResult.rerankEnabled(),

                traceResult.rerankModel(),

                preRerankScoreLabel,

                finalScoreLabel,

                preRerankHits.size(),

                preRerankHits,

                previewHits,

                null);

    }

    private static RagRetrievalPreviewResponse calendarYearPreviewResponse(
            String question, String conversationQuery, int effectiveTopK) {
        String q = question == null ? "" : question.strip();
        return new RagRetrievalPreviewResponse(
                q,
                conversationQuery,
                q,
                "",
                effectiveTopK,
                false,
                0,
                false,
                null,
                null,
                null,
                0,
                List.of(),
                List.of(),
                "历法锚点问题：将依据系统当前日期作答，不走向量检索");
    }



    private record PreviewTraceResult(

            List<SearchHit> hits,

            List<SearchHit> preRerankHits,

            boolean rerankEnabled,

            String rerankModel,

            boolean hybridUsed) {



        List<SearchHit> allHits() {

            List<SearchHit> combined = new ArrayList<>(hits);

            combined.addAll(preRerankHits);

            return combined;

        }

    }



    private List<SearchHit> retrieveUncached(

            RagChatRequest request,

            String searchQuery,

            String keywordQuery,

            int topK) {

        SearchRequest primary = toSearchRequest(request, topK, searchQuery);

        List<SearchHit> hits = new ArrayList<>(searchService.searchForRag(primary, request.minScore()).hits());

        if (!keywordQuery.isBlank() && !keywordQuery.equals(searchQuery.strip())) {

            int expandedTopK = Math.min(topK * 2, 50);

            SearchRequest keywordReq = toSearchRequest(request, expandedTopK, keywordQuery);

            List<SearchHit> keywordHits = searchService.searchForRag(keywordReq, request.minScore()).hits();

            hits = RagHitMerger.merge(hits, keywordHits, expandedTopK);

        }

        return RetrievalHitFilter.preferContentChunks(hits, topK);

    }



    private PreviewTraceResult retrieveUncachedWithTrace(

            RagChatRequest request,

            String searchQuery,

            String keywordQuery,

            int topK) {

        SearchRequest primary = toSearchRequest(request, topK, searchQuery);

        RagSearchTrace trace = searchService.searchForRagWithTrace(primary, request.minScore());

        List<SearchHit> hits = new ArrayList<>(trace.hits());

        List<SearchHit> preRerankHits = trace.preRerankHits();



        if (!keywordQuery.isBlank() && !keywordQuery.equals(searchQuery.strip())) {

            int expandedTopK = Math.min(topK * 2, 50);

            SearchRequest keywordReq = toSearchRequest(request, expandedTopK, keywordQuery);

            List<SearchHit> keywordHits = searchService.searchForRag(keywordReq, request.minScore()).hits();

            hits = RagHitMerger.merge(hits, keywordHits, expandedTopK);

        }

        hits = RetrievalHitFilter.preferContentChunks(hits, topK);

        return new PreviewTraceResult(

                hits,

                preRerankHits,

                trace.rerankEnabled(),

                trace.rerankModel(),

                trace.hybridUsed());

    }



    private List<RagRetrievalPreviewHit> toPreviewHits(
            UUID libraryId, List<SearchHit> hits, Map<UUID, String> fileNames) {

        List<RagRetrievalPreviewHit> previewHits = new ArrayList<>();

        for (int i = 0; i < hits.size(); i++) {

            SearchHit hit = hits.get(i);

            previewHits.add(new RagRetrievalPreviewHit(

                    i + 1,

                    hit.chunkId(),

                    hit.docId(),

                    fileNames.getOrDefault(hit.docId(), ""),

                    hit.chunkIndex(),

                    hit.score(),

                    promptBuilder.excerpt(hit.content()),

                    WeeklyReportWorkItemExtractor.isHeaderOnlyChunk(hit.content()),

                    hit.chunkProfileId(),
                    chunkProfileService.isPrimaryProfile(libraryId, hit.chunkProfileId())));

        }

        return previewHits;

    }



    private static String resolveKeywordQuery(String originalQuestion, String searchQuery, String conversationQuery) {
        if (RagQuestionAnalyzer.isCalendarYearQuestion(originalQuestion)) {
            return RagSearchQueryEnhancer.toKeywordQuery(originalQuestion);
        }

        String effectiveSearchQuery = searchQuery;

        if (RagQueryRewriteService.looksLikeLibraryMetadataRewrite(searchQuery)) {

            effectiveSearchQuery = originalQuestion;

        }

        String fromQuestion = RagSearchQueryEnhancer.toKeywordQuery(originalQuestion);

        String fromRewrite = RagSearchQueryEnhancer.toKeywordQuery(effectiveSearchQuery);

        if (!fromRewrite.isBlank() && !fromRewrite.equals(effectiveSearchQuery.strip())) {

            return RagSearchQueryEnhancer.mergeKeywordQueries(fromQuestion, fromRewrite);

        }

        if (!fromQuestion.isBlank()) {

            return fromQuestion;

        }

        return RagSearchQueryEnhancer.toKeywordQuery(conversationQuery);

    }



    private int resolveEffectiveTopK(String question, int topK) {

        if (RagQuestionAnalyzer.isSynthesisQuestion(question)) {

            return Math.min(Math.max(topK * 2, 10), 20);

        }

        return topK;

    }



    private Map<UUID, String> resolveFileNames(List<SearchHit> hits) {

        List<UUID> docIds = hits.stream().map(SearchHit::docId).distinct().collect(Collectors.toList());

        return docMetadataStore.findFileNamesByDocIds(docIds);

    }



    private static SearchRequest toSearchRequest(RagChatRequest request, int topK, String searchQuery) {

        SearchRequest.SearchFilter filter = null;

        if (request.filter() != null) {

            filter = new SearchRequest.SearchFilter(request.filter().docIds(), request.filter().metadata());

        }

        return new SearchRequest(
                request.libraryId(),
                request.tenantId(),
                searchQuery,
                topK,
                filter,
                request.includeAllChunkProfiles(),
                request.chunkProfileIds());

    }

}


