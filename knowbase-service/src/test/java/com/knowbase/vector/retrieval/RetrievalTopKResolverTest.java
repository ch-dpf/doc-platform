package com.knowbase.vector.retrieval;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.knowbase.library.config.RetrievalRulesSettings;
import com.knowbase.vector.config.RagProperties;
import org.junit.jupiter.api.Test;

class RetrievalTopKResolverTest {

    @Test
    void prefersRequestThenLibraryThenGlobal() {
        var retrieval = new RetrievalRulesSettings();
        retrieval.setDefaultTopK(8);
        var rag = new RagProperties();
        rag.setDefaultTopK(5);

        assertEquals(15, RetrievalTopKResolver.resolve(15, retrieval, rag));
        assertEquals(8, RetrievalTopKResolver.resolve(null, retrieval, rag));
        retrieval.setDefaultTopK(0);
        assertEquals(5, RetrievalTopKResolver.resolve(null, retrieval, rag));
    }
}
