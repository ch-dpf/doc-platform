package com.knowbase.vector.controller;

import com.knowbase.vector.dto.SearchRequest;
import com.knowbase.vector.dto.SearchResponse;
import com.knowbase.vector.service.VectorSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "混合检索", description = "向量检索 + 关键字全文检索（RRF 融合）")
@RestController
@RequestMapping("/api/v1/vector-libraries")
public class SearchController {

    private final VectorSearchService searchService;

    public SearchController(VectorSearchService searchService) {
        this.searchService = searchService;
    }

    @Operation(summary = "混合检索预览", description = "对知识库执行向量 + 关键字混合检索，返回 Top-N 片段")
    @PostMapping("/{libraryId}/search")
    public SearchResponse search(
            @PathVariable UUID libraryId,
            @Valid @RequestBody SearchRequest request) {
        if (!libraryId.equals(request.libraryId())) {
            throw new IllegalArgumentException("libraryId 与请求体不一致");
        }
        return searchService.search(request);
    }
}
