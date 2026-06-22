package com.knowbase.retrieval;

import java.util.List;
import java.util.Map;

public interface RetrievalPostProcessor {

    List<RetrievalCandidate> fuse(List<RetrievalCandidate> candidates, Map<String, Object> retrievalPolicy);

    List<RetrievalCandidate> rerank(List<RetrievalCandidate> candidates, Map<String, Object> retrievalPolicy);

    default List<RetrievalCandidate> process(List<RetrievalCandidate> candidates, Map<String, Object> retrievalPolicy) {
        return rerank(fuse(candidates, retrievalPolicy), retrievalPolicy);
    }
}
