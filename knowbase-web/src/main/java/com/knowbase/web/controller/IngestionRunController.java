package com.knowbase.web.controller;

import com.knowbase.api.command.CreateIngestionRunCommand;
import com.knowbase.api.result.IngestionRunResult;
import com.knowbase.application.usecase.RunIngestionUseCase;
import com.knowbase.web.support.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "入库任务", description = "文档入库与索引构建接口")
@RestController
@RequestMapping("/api/v1")
public class IngestionRunController {

    private final RunIngestionUseCase runIngestionUseCase;

    public IngestionRunController(RunIngestionUseCase runIngestionUseCase) {
        this.runIngestionUseCase = runIngestionUseCase;
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

    @Operation(summary = "获取入库任务详情", description = "根据入库任务 ID 查询执行状态与统计信息")
    @GetMapping("/ingestion-runs/{runId}")
    public ApiResponse<IngestionRunResult> get(
            @Parameter(description = "入库任务 ID") @PathVariable UUID runId
    ) {
        return ApiResponse.ok(runIngestionUseCase.get(runId));
    }
}
