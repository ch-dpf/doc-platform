package com.knowbase.web.controller;

import com.knowbase.api.result.DocumentChunkResult;
import com.knowbase.api.result.IndexVersionResult;
import com.knowbase.api.result.KnowledgeDocumentResult;
import com.knowbase.application.service.DefaultLibraryCatalogService;
import com.knowbase.web.support.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "知识库索引与文档", description = "索引版本、文档与块查询")
@RestController
@RequestMapping("/api/v1/libraries/{libraryId}")
public class LibraryCatalogController {

    private final DefaultLibraryCatalogService catalogService;

    public LibraryCatalogController(DefaultLibraryCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @Operation(summary = "查询索引版本列表")
    @GetMapping("/index-versions")
    public ApiResponse<List<IndexVersionResult>> listIndexVersions(@PathVariable UUID libraryId) {
        return ApiResponse.ok(catalogService.listIndexVersions(libraryId));
    }

    @Operation(summary = "查询索引版本详情")
    @GetMapping("/index-versions/{indexVersionId}")
    public ApiResponse<IndexVersionResult> getIndexVersion(
            @PathVariable UUID libraryId,
            @PathVariable UUID indexVersionId
    ) {
        return ApiResponse.ok(catalogService.getIndexVersion(libraryId, indexVersionId));
    }

    @Operation(summary = "查询文档列表")
    @GetMapping("/documents")
    public ApiResponse<List<KnowledgeDocumentResult>> listDocuments(
            @PathVariable UUID libraryId,
            @RequestParam(required = false) UUID indexVersionId
    ) {
        return ApiResponse.ok(catalogService.listDocuments(libraryId, indexVersionId));
    }

    @Operation(summary = "查询文档详情")
    @GetMapping("/documents/{documentId}")
    public ApiResponse<KnowledgeDocumentResult> getDocument(
            @PathVariable UUID libraryId,
            @PathVariable UUID documentId
    ) {
        return ApiResponse.ok(catalogService.getDocument(libraryId, documentId));
    }

    @Operation(summary = "查询文档块列表")
    @GetMapping("/documents/{documentId}/chunks")
    public ApiResponse<List<DocumentChunkResult>> listDocumentChunks(
            @PathVariable UUID libraryId,
            @PathVariable UUID documentId
    ) {
        return ApiResponse.ok(catalogService.listDocumentChunks(libraryId, documentId));
    }
}
