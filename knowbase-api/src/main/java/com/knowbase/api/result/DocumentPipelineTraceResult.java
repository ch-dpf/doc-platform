package com.knowbase.api.result;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "文档最近一次入库 Pipeline Trace 定位信息")
public record DocumentPipelineTraceResult(
        @Schema(description = "入库任务 ID")
        UUID runId,
        @Schema(description = "Trace ID，可用于观测页查询 Span 树")
        UUID traceId,
        @Schema(description = "索引作业状态")
        String jobStatus,
        @Schema(description = "索引作业阶段")
        String jobStage,
        @Schema(description = "入库产生的分块数")
        int chunkCount
) {
}
