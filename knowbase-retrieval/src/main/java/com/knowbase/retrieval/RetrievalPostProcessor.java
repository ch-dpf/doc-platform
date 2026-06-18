package com.knowbase.retrieval;

import java.util.List;
import java.util.Map;

public interface RetrievalPostProcessor {

    List<RetrievalCandidate> process(List<RetrievalCandidate> candidates, Map<String, Object> retrievalPolicy);
}
