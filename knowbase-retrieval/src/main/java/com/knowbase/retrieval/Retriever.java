package com.knowbase.retrieval;

import java.util.List;

public interface Retriever {

    List<RetrievalCandidate> retrieve(RetrievalRequest request);
}
