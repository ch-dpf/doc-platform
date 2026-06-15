package com.knowbase.vector.retrieval;

import com.knowbase.library.config.RetrievalRulesSettings;
import com.knowbase.vector.config.RagProperties;

/**
 * 相似度阈值优先级：请求覆盖 &gt; 库级配置 &gt; 全局 rag.min-score。
 */
public final class RetrievalMinScoreResolver {

    private RetrievalMinScoreResolver() {}

    public static double resolve(
            Double requestOverride,
            RetrievalRulesSettings retrieval,
            RagProperties ragProperties) {
        if (requestOverride != null) {
            return requestOverride;
        }
        if (retrieval != null && retrieval.getSimilarityThreshold() > 0) {
            return retrieval.getSimilarityThreshold();
        }
        return ragProperties.getMinScore();
    }
}
