package com.docplatform.vector.service;

import com.docplatform.vector.client.OllamaEmbeddingClient;
import com.docplatform.vector.dto.SearchRequest;
import com.docplatform.vector.dto.SearchResponse;
import com.docplatform.vector.mapper.DocumentChunkMapper;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class VectorSearchService {

    private final DocumentChunkMapper chunkMapper;
    private final OllamaEmbeddingClient embeddingClient;

    public VectorSearchService(DocumentChunkMapper chunkMapper, OllamaEmbeddingClient embeddingClient) {
        this.chunkMapper = chunkMapper;
        this.embeddingClient = embeddingClient;
    }

    public SearchResponse search(SearchRequest request) {
        float[] queryVector = embeddingClient.embed(request.query());
        List<UUID> docIds = request.filter() != null && request.filter().docIds() != null
                ? request.filter().docIds()
                : Collections.emptyList();
        return new SearchResponse(chunkMapper.search(
                request.tenantId(),
                queryVector,
                request.topK(),
                docIds.isEmpty() ? null : docIds));
    }
}
