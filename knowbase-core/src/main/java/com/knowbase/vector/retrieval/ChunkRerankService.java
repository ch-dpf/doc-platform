package com.knowbase.vector.retrieval;

import com.knowbase.library.config.RetrievalRulesSettings;
import com.knowbase.vector.chunk.VectorSimilarity;
import com.knowbase.vector.config.RetrievalProperties;
import com.knowbase.vector.dto.SearchHit;
import com.knowbase.vector.service.LibraryEmbeddingService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 对候选片段用 Embedding 模型重算 query–chunk 余弦相似度并重新排序。
 * rerankModel 为空时使用库级 Embedding 模型；否则使用指定 Ollama 模型。
 */
@Service
public class ChunkRerankService {

    private final LibraryEmbeddingService libraryEmbeddingService;
    private final RetrievalProperties retrievalProperties;

    public ChunkRerankService(
            LibraryEmbeddingService libraryEmbeddingService,
            RetrievalProperties retrievalProperties) {
        this.libraryEmbeddingService = libraryEmbeddingService;
        this.retrievalProperties = retrievalProperties;
    }

    public List<SearchHit> rerank(
            UUID libraryId,
            String query,
            List<SearchHit> candidates,
            RetrievalRulesSettings retrieval,
            int topK) {
        if (candidates == null || candidates.isEmpty() || topK <= 0) {
            return List.of();
        }
        int poolSize = Math.min(candidates.size(), retrievalProperties.getMaxRerankCandidates());
        List<SearchHit> pool = candidates.subList(0, poolSize);

        String rerankModel = resolveRerankModel(retrieval);
        float[] queryVector = libraryEmbeddingService.embedWithModel(libraryId, query, rerankModel);
        List<String> texts = pool.stream().map(SearchHit::content).toList();
        List<float[]> chunkVectors = libraryEmbeddingService.embedBatchWithModel(libraryId, texts, rerankModel);

        List<SearchHit> rescored = new ArrayList<>(pool.size());
        for (int i = 0; i < pool.size(); i++) {
            SearchHit hit = pool.get(i);
            double score = VectorSimilarity.cosineSimilarity(queryVector, chunkVectors.get(i));
            rescored.add(new SearchHit(
                    hit.chunkId(),
                    hit.docId(),
                    hit.tenantId(),
                    hit.version(),
                    hit.chunkIndex(),
                    hit.content(),
                    score,
                    hit.parentContext(),
                    hit.chunkProfileId()));
        }
        rescored.sort(Comparator.comparingDouble(SearchHit::score).reversed());
        if (rescored.size() <= topK) {
            return rescored;
        }
        return List.copyOf(rescored.subList(0, topK));
    }

    private String resolveRerankModel(RetrievalRulesSettings retrieval) {
        if (retrieval != null && retrieval.getRerankModel() != null && !retrieval.getRerankModel().isBlank()) {
            return retrieval.getRerankModel().trim();
        }
        return null;
    }
}
