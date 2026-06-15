package com.knowbase.facade;

import com.knowbase.api.command.SearchCommand;
import com.knowbase.api.facade.KnowbaseSearchFacade;
import com.knowbase.api.result.SearchHitResult;
import com.knowbase.api.result.SearchResult;
import com.knowbase.vector.dto.SearchHit;
import com.knowbase.vector.dto.SearchRequest;
import com.knowbase.vector.dto.SearchResponse;
import com.knowbase.vector.service.VectorSearchService;

import java.util.List;

public class KnowbaseSearchFacadeImpl implements KnowbaseSearchFacade {

    private final VectorSearchService searchService;
    private final KnowbaseTenantSupport tenantSupport;

    public KnowbaseSearchFacadeImpl(VectorSearchService searchService, KnowbaseTenantSupport tenantSupport) {
        this.searchService = searchService;
        this.tenantSupport = tenantSupport;
    }

    @Override
    public SearchResult search(SearchCommand command) {
        String tenantId = tenantSupport.resolve(command.tenantId());
        SearchRequest request = new SearchRequest(
                command.libraryId(), tenantId, command.query(), command.topK(), null);
        SearchResponse response = searchService.search(request);
        return new SearchResult(mapHits(response.hits()), response.hits().size());
    }

    private static List<SearchHitResult> mapHits(List<SearchHit> hits) {
        if (hits == null) {
            return List.of();
        }
        return hits.stream()
                .map(h -> new SearchHitResult(
                        h.chunkId(),
                        h.docId(),
                        h.chunkIndex(),
                        h.score(),
                        h.content(),
                        h.chunkProfileId()))
                .toList();
    }
}
