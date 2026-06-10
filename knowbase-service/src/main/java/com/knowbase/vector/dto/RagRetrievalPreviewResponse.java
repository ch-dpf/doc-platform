package com.knowbase.vector.dto;

import java.util.List;

public record RagRetrievalPreviewResponse(
        String question,
        String conversationQuery,
        String searchQuery,
        String keywordQuery,
        int effectiveTopK,
        boolean cacheHit,
        int hitCount,
        boolean rerankEnabled,
        String rerankModel,
        String preRerankScoreLabel,
        String finalScoreLabel,
        int preRerankHitCount,
        List<RagRetrievalPreviewHit> preRerankHits,
        List<RagRetrievalPreviewHit> hits,
        /** 非空时表示无需/不应走向量检索（如历法锚点问题） */
        String retrievalNote
) {}
