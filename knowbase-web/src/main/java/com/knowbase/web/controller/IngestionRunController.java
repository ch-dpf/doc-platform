package com.knowbase.web.controller;

import com.knowbase.api.command.CreateIngestionRunCommand;
import com.knowbase.api.command.PrepareIngestionCommand;
import com.knowbase.api.command.PreviewIngestionCommand;
import com.knowbase.api.result.BatchObjectUploadResult;
import com.knowbase.api.result.IngestionPrepareResult;
import com.knowbase.api.result.IngestionPreviewResult;
import com.knowbase.api.result.IngestionRunResult;
import com.knowbase.api.result.ObjectUploadResult;
import com.knowbase.application.service.DefaultObjectUploadService;
import com.knowbase.application.usecase.PrepareIngestionUseCase;
import com.knowbase.application.usecase.PreviewIngestionUseCase;
import com.knowbase.application.usecase.RunIngestionUseCase;
import com.knowbase.web.support.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "入库任务", description = "文档入库与索引构建接口")
@RestController
@RequestMapping("/api/v1")
public class IngestionRunController {

    private final RunIngestionUseCase runIngestionUseCase;
    private final PreviewIngestionUseCase previewIngestionUseCase;
    private final PrepareIngestionUseCase prepareIngestionUseCase;
    private final DefaultObjectUploadService uploadService;

    public IngestionRunController(
            RunIngestionUseCase runIngestionUseCase,
            PreviewIngestionUseCase previewIngestionUseCase,
            PrepareIngestionUseCase prepareIngestionUseCase,
            DefaultObjectUploadService uploadService
    ) {
        this.runIngestionUseCase = runIngestionUseCase;
        this.previewIngestionUseCase = previewIngestionUseCase;
        this.prepareIngestionUseCase = prepareIngestionUseCase;
        this.uploadService = uploadService;
    }

    @Operation(summary = "创建入库任务", description = "向指定知识库提交文档入库任务，支持批量源文件 URI")
    @PostMapping("/libraries/{libraryId}/ingestion-runs")
    public ApiResponse<IngestionRunResult> create(
            @Parameter(description = "知识库 ID") @PathVariable UUID libraryId,
            @Valid @RequestBody CreateIngestionRunCommand command
    ) {
        CreateIngestionRunCommand normalized = new CreateIngestionRunCommand(
                libraryId,
                command.sourceUris(),
                command.sourceType(),
                command.documentProfileCode(),
                command.publishIndexOnSuccess(),
                command.options()
        );
        return ApiResponse.ok(runIngestionUseCase.create(normalized));
    }

    @Operation(summary = "上传并创建入库任务", description = "批量上传文件到对象存储后自动创建入库任务")
    @PostMapping(value = "/libraries/{libraryId}/ingestion-runs/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UploadAndIngestResponse> uploadAndCreate(
            @Parameter(description = "知识库 ID") @PathVariable UUID libraryId,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(required = false) String bucket,
            @RequestParam(required = false) String documentProfileCode,
            @RequestParam(defaultValue = "true") boolean publishIndexOnSuccess,
            @RequestParam(defaultValue = "true") boolean autoStart,
            @RequestParam(required = false) Integer maxFiles
    ) throws Exception {
        List<DefaultObjectUploadService.UploadCandidate> candidates = new ArrayList<>();
        for (MultipartFile file : files) {
            candidates.add(new DefaultObjectUploadService.UploadCandidate(
                    file.getOriginalFilename(),
                    file.getInputStream(),
                    file.getContentType(),
                    file.getSize()
            ));
        }
        BatchObjectUploadResult uploadResult = uploadService.uploadBatch(bucket, candidates);
        List<String> sourceUris = uploadResult.uploaded().stream().map(ObjectUploadResult::uri).toList();
        IngestionRunResult ingestionRun = null;
        if (autoStart && !sourceUris.isEmpty()) {
            Map<String, Object> options = new HashMap<>();
            if (maxFiles != null) {
                options.put("maxFiles", maxFiles);
            }
            ingestionRun = runIngestionUseCase.create(new CreateIngestionRunCommand(
                    libraryId,
                    sourceUris,
                    "minio",
                    documentProfileCode,
                    publishIndexOnSuccess,
                    options
            ));
        }
        return ApiResponse.ok(new UploadAndIngestResponse(uploadResult, ingestionRun));
    }

    @Operation(summary = "入库分段预览", description = "解析并切块预览，不写入索引")
    @PostMapping("/libraries/{libraryId}/ingestion/preview")
    public ApiResponse<IngestionPreviewResult> preview(
            @Parameter(description = "知识库 ID") @PathVariable UUID libraryId,
            @Valid @RequestBody PreviewIngestionCommand command
    ) {
        PreviewIngestionCommand normalized = new PreviewIngestionCommand(
                libraryId,
                command.sourceUris(),
                command.documentProfileCode(),
                command.options()
        );
        return ApiResponse.ok(previewIngestionUseCase.preview(normalized));
    }

    @Operation(summary = "入库准备（解析/清洗/分段）", description = "在向量化前分阶段执行结构感知解析、文本清洗与分段切块")
    @PostMapping("/libraries/{libraryId}/ingestion/prepare")
    public ApiResponse<IngestionPrepareResult> prepare(
            @Parameter(description = "知识库 ID") @PathVariable UUID libraryId,
            @Valid @RequestBody PrepareIngestionCommand command
    ) {
        return ApiResponse.ok(prepareIngestionUseCase.prepare(normalizePrepareCommand(libraryId, command)));
    }

    @Operation(summary = "结构感知解析预览", description = "仅执行解析阶段，返回结构块与 parser 信息")
    @PostMapping("/libraries/{libraryId}/ingestion/prepare/parse")
    public ApiResponse<IngestionPrepareResult> prepareParse(
            @Parameter(description = "知识库 ID") @PathVariable UUID libraryId,
            @Valid @RequestBody PrepareIngestionCommand command
    ) {
        return ApiResponse.ok(prepareIngestionUseCase.prepare(normalizePrepareCommand(libraryId, command, "parse")));
    }

    @Operation(summary = "文本清洗规范化预览", description = "执行解析 + 清洗，返回规范化统计与预览文本")
    @PostMapping("/libraries/{libraryId}/ingestion/prepare/normalize")
    public ApiResponse<IngestionPrepareResult> prepareNormalize(
            @Parameter(description = "知识库 ID") @PathVariable UUID libraryId,
            @Valid @RequestBody PrepareIngestionCommand command
    ) {
        return ApiResponse.ok(prepareIngestionUseCase.prepare(normalizePrepareCommand(libraryId, command, "normalize")));
    }

    @Operation(summary = "分段切块预览", description = "执行解析 + 清洗 + 分段，不写入索引")
    @PostMapping("/libraries/{libraryId}/ingestion/prepare/chunk")
    public ApiResponse<IngestionPrepareResult> prepareChunk(
            @Parameter(description = "知识库 ID") @PathVariable UUID libraryId,
            @Valid @RequestBody PrepareIngestionCommand command
    ) {
        return ApiResponse.ok(prepareIngestionUseCase.prepare(normalizePrepareCommand(libraryId, command, "chunk")));
    }

    private static PrepareIngestionCommand normalizePrepareCommand(UUID libraryId, PrepareIngestionCommand command) {
        return normalizePrepareCommand(libraryId, command, command.prepareStage());
    }

    private static PrepareIngestionCommand normalizePrepareCommand(
            UUID libraryId,
            PrepareIngestionCommand command,
            String prepareStage
    ) {
        return new PrepareIngestionCommand(
                libraryId,
                command.sourceUris(),
                command.documentProfileCode(),
                prepareStage == null || prepareStage.isBlank() ? "all" : prepareStage,
                command.options()
        );
    }

    @Operation(summary = "获取入库任务详情", description = "根据入库任务 ID 查询执行状态与统计信息")
    @GetMapping("/ingestion-runs/{runId}")
    public ApiResponse<IngestionRunResult> get(
            @Parameter(description = "入库任务 ID") @PathVariable UUID runId
    ) {
        return ApiResponse.ok(runIngestionUseCase.get(runId));
    }

    public record UploadAndIngestResponse(
            BatchObjectUploadResult upload,
            IngestionRunResult ingestionRun
    ) {
    }
}
