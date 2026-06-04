package com.docplatform.vector.controller;

import com.docplatform.vector.dto.SearchRequest;
import com.docplatform.vector.dto.SearchResponse;
import com.docplatform.vector.service.VectorSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "语义检索", description = "向量相似度检索 API")
@RestController
@RequestMapping("/api/v1")
public class SearchController {

    private final VectorSearchService searchService;

    public SearchController(VectorSearchService searchService) {
        this.searchService = searchService;
    }

    @Operation(summary = "语义检索", description = "将 query 向量化后在 pgvector 中检索 TopK 片段")
    @PostMapping("/search")
    public SearchResponse search(@Valid @RequestBody SearchRequest request) {
        return searchService.search(request);
    }
}
