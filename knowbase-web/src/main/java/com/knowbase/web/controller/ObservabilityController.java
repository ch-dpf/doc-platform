package com.knowbase.web.controller;

import com.knowbase.api.command.CreateEvalRunCommand;
import com.knowbase.api.result.EvalRunResult;
import com.knowbase.api.result.PipelineSpanResult;
import com.knowbase.application.service.DefaultEvalService;
import com.knowbase.application.service.DefaultObservabilityService;
import com.knowbase.web.support.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "观测与评测", description = "Pipeline Trace 与评测运行")
@RestController
@RequestMapping("/api/v1/observability")
public class ObservabilityController {

    private final DefaultObservabilityService observabilityService;
    private final DefaultEvalService evalService;

    public ObservabilityController(
            DefaultObservabilityService observabilityService,
            DefaultEvalService evalService
    ) {
        this.observabilityService = observabilityService;
        this.evalService = evalService;
    }

    @Operation(summary = "按 traceId 查询 Pipeline Span")
    @GetMapping("/traces/{traceId}")
    public ApiResponse<List<PipelineSpanResult>> listTrace(@PathVariable UUID traceId) {
        return ApiResponse.ok(observabilityService.listTrace(traceId));
    }

    @Operation(summary = "按 pipeline/runId 查询 Pipeline Span")
    @GetMapping("/pipelines/{pipeline}/runs/{runId}")
    public ApiResponse<List<PipelineSpanResult>> listPipelineRun(
            @PathVariable String pipeline,
            @PathVariable UUID runId
    ) {
        return ApiResponse.ok(observabilityService.listPipelineRun(pipeline, runId));
    }

    @Operation(summary = "创建评测运行")
    @PostMapping("/eval-runs")
    public ApiResponse<EvalRunResult> createEvalRun(@Valid @RequestBody CreateEvalRunCommand command) {
        return ApiResponse.ok(evalService.create(command));
    }

    @Operation(summary = "查询评测运行详情")
    @GetMapping("/eval-runs/{evalRunId}")
    public ApiResponse<EvalRunResult> getEvalRun(@PathVariable UUID evalRunId) {
        return ApiResponse.ok(evalService.get(evalRunId));
    }

    @Operation(summary = "查询评测运行列表")
    @GetMapping("/eval-runs")
    public ApiResponse<List<EvalRunResult>> listEvalRuns(
            @RequestParam String tenantId,
            @RequestParam(required = false) UUID agentId
    ) {
        return ApiResponse.ok(evalService.list(tenantId, agentId));
    }
}
