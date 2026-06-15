package com.knowbase.vector.dto;

import java.util.List;

/** RAG 检索链路调试信息：重排前后的候选与分数类型。 */
public record RagSearchTrace(
        List<SearchHit> hits,
        List<SearchHit> preRerankHits,
        boolean rerankEnabled,
        String rerankModel,
        boolean hybridUsed
) {
}
