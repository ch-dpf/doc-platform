package com.knowbase.vector.controller;

import com.knowbase.library.dto.FailedBatchJobItemsResponse;
import com.knowbase.library.dto.LibraryBatchJobResponse;
import com.knowbase.library.dto.RetryBatchJobResponse;
import com.knowbase.library.service.LibraryBatchJobRetryService;
import com.knowbase.library.service.LibraryBatchJobService;
import com.knowbase.vector.dto.ChunkPreviewRequest;
import com.knowbase.vector.dto.ChunkPreviewResponse;
import com.knowbase.vector.dto.RebuildCandidatesResponse;
import com.knowbase.vector.dto.RebuildLibraryRequest;
import com.knowbase.vector.dto.RebuildLibraryResponse;
import com.knowbase.vector.service.ChunkPreviewService;
import com.knowbase.vector.service.LibraryRebuildService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "索引管理", description = "向量索引补偿与清理")
@RestController
@RequestMapping("/api/v1/index")
public class IndexAdminController {

    private final ChunkPreviewService chunkPreviewService;
    private final LibraryRebuildService libraryRebuildService;
    private final LibraryBatchJobService batchJobService;
    private final LibraryBatchJobRetryService batchJobRetryService;

    public IndexAdminController(
            ChunkPreviewService chunkPreviewService,
            LibraryRebuildService libraryRebuildService,
            LibraryBatchJobService batchJobService,
            LibraryBatchJobRetryService batchJobRetryService) {
        this.chunkPreviewService = chunkPreviewService;
        this.libraryRebuildService = libraryRebuildService;
        this.batchJobService = batchJobService;
        this.batchJobRetryService = batchJobRetryService;
    }

    @Operation(summary = "分块规则预览", description = "根据样本文本预览解析清洗后的分块结果（不入库）")
    @PostMapping("/chunk-preview")
    public ChunkPreviewResponse chunkPreview(@Valid @RequestBody ChunkPreviewRequest request) {
        return chunkPreviewService.preview(request);
    }

    @Operation(
            summary = "批量重索引候选统计",
            description = "统计可重索引文档数；可选 chunkProfileId 仅统计该分块档。")
    @GetMapping("/rebuild-library/candidates")
    public RebuildCandidatesResponse rebuildCandidates(
            @RequestParam UUID libraryId,
            @RequestParam String tenantId,
            @RequestParam(required = false) String chunkProfileId) {
        return libraryRebuildService.countCandidates(libraryId, tenantId, chunkProfileId);
    }

    @Operation(
            summary = "批量补偿重索引",
            description = "按当前知识库规则，对已解析文档异步重新分块并向量化；可选 chunkProfileId 仅处理该档文档。")
    @PostMapping("/rebuild-library")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RebuildLibraryResponse rebuildLibrary(@Valid @RequestBody RebuildLibraryRequest request) {
        return libraryRebuildService.scheduleRebuild(
                request.libraryId(), request.tenantId(), request.chunkProfileId());
    }

    @Operation(summary = "查询批量任务进度", description = "重索引或分块档归档任务的进度与状态。")
    @GetMapping("/batch-jobs/{jobId}")
    public LibraryBatchJobResponse getBatchJob(@PathVariable UUID jobId) {
        return batchJobService.get(jobId);
    }

    @Operation(summary = "批量任务失败文档列表", description = "返回失败项的文档 ID 与文件名。")
    @GetMapping("/batch-jobs/{jobId}/failed-items")
    public FailedBatchJobItemsResponse listFailedBatchJobItems(@PathVariable UUID jobId) {
        return batchJobService.listFailedItems(jobId);
    }

    @Operation(summary = "列出知识库批量任务", description = "按创建时间倒序返回最近批量任务。")
    @GetMapping("/batch-jobs")
    public List<LibraryBatchJobResponse> listBatchJobs(
            @RequestParam UUID libraryId,
            @RequestParam(required = false) String tenantId,
            @RequestParam(defaultValue = "10") int limit) {
        return batchJobService.listByLibrary(libraryId, tenantId, limit);
    }

    @Operation(summary = "重试失败项", description = "对 PARTIAL/FAILED 任务中记录的失败文档重新提交批量任务。")
    @PostMapping("/batch-jobs/{jobId}/retry")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RetryBatchJobResponse retryBatchJob(@PathVariable UUID jobId) {
        return batchJobRetryService.retry(jobId);
    }
}
