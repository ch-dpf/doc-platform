package com.knowbase.vector.retrieval;

import com.knowbase.library.config.RetrievalRulesSettings;
import com.knowbase.vector.config.RagProperties;

/**
 * Top K 优先级：请求覆盖 &gt; 库级 defaultTopK &gt; 全局 rag.default-top-k。
 */
public final class RetrievalTopKResolver {

    private RetrievalTopKResolver() {}

    public static int resolve(Integer requestTopK, RetrievalRulesSettings retrieval, RagProperties ragProperties) {
        if (requestTopK != null) {
            return requestTopK;
        }
        if (retrieval != null && retrieval.getDefaultTopK() > 0) {
            return retrieval.getDefaultTopK();
        }
        return ragProperties.getDefaultTopK();
    }
}
