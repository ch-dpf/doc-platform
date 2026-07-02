package com.knowbase.web.controller;

import com.knowbase.api.result.IngestionDocumentErrorResult;
import com.knowbase.application.service.DefaultLibraryCatalogService;
import com.knowbase.web.support.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "入库错误", description = "入库运行文档级错误查询")
@RestController
@RequestMapping("/api/v1/ingestion-runs")
public class IngestionErrorController {

    private final DefaultLibraryCatalogService catalogService;

    public IngestionErrorController(DefaultLibraryCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @Operation(summary = "查询入库文档错误")
    @GetMapping("/{runId}/errors")
    public ApiResponse<List<IngestionDocumentErrorResult>> listErrors(@PathVariable UUID runId) {
        return ApiResponse.ok(catalogService.listIngestionErrors(runId));
    }
}
