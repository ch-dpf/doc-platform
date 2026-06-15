package com.knowbase.vector.retrieval;

import com.knowbase.library.config.RetrievalRulesSettings;
import com.knowbase.library.service.VectorLibraryService;
import com.knowbase.vector.config.RetrievalProperties;
import com.knowbase.vector.dto.SearchHit;
import com.knowbase.vector.service.LibraryEmbeddingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChunkRerankServiceTest {

    private static final UUID LIBRARY_ID = VectorLibraryService.DEFAULT_LIBRARY_ID;
    private static final UUID DOC = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private LibraryEmbeddingService libraryEmbeddingService;

    @InjectMocks
    private ChunkRerankService chunkRerankService;

    @Test
    void rerankOrdersByCosineSimilarity() {
        RetrievalProperties props = new RetrievalProperties();
        props.setMaxRerankCandidates(10);
        chunkRerankService = new ChunkRerankService(libraryEmbeddingService, props);

        UUID low = UUID.fromString("00000000-0000-0000-0000-000000000010");
        UUID high = UUID.fromString("00000000-0000-0000-0000-000000000011");
        List<SearchHit> candidates = List.of(
                new SearchHit(low, DOC, "demo", 1, 0, "low relevance", 0.4),
                new SearchHit(high, DOC, "demo", 1, 1, "high relevance", 0.9));

        when(libraryEmbeddingService.embedWithModel(eq(LIBRARY_ID), eq("query"), isNull()))
                .thenReturn(new float[] {1f, 0f});
        when(libraryEmbeddingService.embedBatchWithModel(eq(LIBRARY_ID), any(), isNull()))
                .thenReturn(List.of(new float[] {0.5f, 0.5f}, new float[] {1f, 0f}));

        List<SearchHit> reranked = chunkRerankService.rerank(
                LIBRARY_ID, "query", candidates, new RetrievalRulesSettings(), 1);

        assertEquals(1, reranked.size());
        assertEquals(high, reranked.get(0).chunkId());
    }
}
