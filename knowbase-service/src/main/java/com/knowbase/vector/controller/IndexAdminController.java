package com.knowbase.vector.controller;

import com.knowbase.vector.dto.ChunkPreviewRequest;
import com.knowbase.vector.dto.ChunkPreviewResponse;
import com.knowbase.vector.dto.RebuildLibraryRequest;
import com.knowbase.vector.dto.RebuildLibraryResponse;
import com.knowbase.vector.service.ChunkPreviewService;
import com.knowbase.vector.service.LibraryRebuildService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "索引管理", description = "向量索引补偿与清理")
@RestController
@RequestMapping("/api/v1/index")
public class IndexAdminController {

    private final ChunkPreviewService chunkPreviewService;
    private final LibraryRebuildService libraryRebuildService;

    public IndexAdminController(
            ChunkPreviewService chunkPreviewService,
            LibraryRebuildService libraryRebuildService) {
        this.chunkPreviewService = chunkPreviewService;
        this.libraryRebuildService = libraryRebuildService;
    }

    @Operation(summary = "分块规则预览", description = "根据样本文本预览解析清洗后的分块结果（不入库）")
    @PostMapping("/chunk-preview")
    public ChunkPreviewResponse chunkPreview(@Valid @RequestBody ChunkPreviewRequest request) {
        return chunkPreviewService.preview(request);
    }

    @Operation(summary = "批量补偿重索引", description = "按当前知识库规则，对已解析文档异步重新分块并向量化")
    @PostMapping("/rebuild-library")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RebuildLibraryResponse rebuildLibrary(@Valid @RequestBody RebuildLibraryRequest request) {
        return libraryRebuildService.scheduleRebuild(request.libraryId(), request.tenantId());
    }
}
