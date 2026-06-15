package com.knowbase.vector.rag;

import com.knowbase.vector.dto.SearchHit;

import java.util.List;

/** 判断检索片段是否与问题关键词有足够重合，避免弱相关片段误导 LLM。 */
public final class RagHitRelevance {

    private RagHitRelevance() {}

    public static boolean hasTermOverlap(String question, List<SearchHit> hits) {
        if (hits == null || hits.isEmpty()) {
            return false;
        }
        if (RagQuestionAnalyzer.isSynthesisQuestion(question)) {
            return true;
        }
        List<String> terms = RagSearchQueryEnhancer.extractTerms(question);
        if (terms.isEmpty()) {
            return true;
        }
        int required = Math.min(2, terms.size());
        for (SearchHit hit : hits) {
            if (hit.content() == null || hit.content().isBlank()) {
                continue;
            }
            String content = hit.content();
            int matched = 0;
            for (String term : terms) {
                if (content.contains(term)) {
                    matched++;
                }
            }
            if (matched >= required) {
                return true;
            }
        }
        return false;
    }
}
