package com.knowbase.vector.dto;

import java.util.List;

/** RAG 检索 trace：运营调试召回块、分数与改写链路。 */
public record RagRetrievalTrace(
        String conversationQuery,
        String searchQuery,
        String keywordQuery,
        int effectiveTopK,
        boolean cacheHit,
        boolean rerankEnabled,
        String rerankModel,
        String preRerankScoreLabel,
        String finalScoreLabel,
        int hitCount,
        int preRerankHitCount,
        List<RagRetrievalPreviewHit> hits,
        List<RagRetrievalPreviewHit> preRerankHits,
        String retrievalNote
) {}
