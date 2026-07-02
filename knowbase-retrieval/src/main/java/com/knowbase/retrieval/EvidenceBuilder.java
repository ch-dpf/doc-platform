package com.knowbase.retrieval;

import com.knowbase.domain.model.EvidencePack;

import java.util.List;

public interface EvidenceBuilder {

    EvidencePack build(List<RetrievalCandidate> candidates);
}
