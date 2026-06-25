package com.knowbase.api.result;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "分块后处理阶段结果")
public record PostProcessStageResult(
        @Schema(description = "是否执行了后处理")
        boolean applied,
        @Schema(description = "后处理前分块总数")
        int beforeCount,
        @Schema(description = "后处理后分块总数")
        int afterCount,
        @Schema(description = "后处理前可索引分块数")
        int indexableBeforeCount,
        @Schema(description = "后处理后可索引分块数")
        int indexableAfterCount,
        @Schema(description = "新增摘要分块数")
        int summariesAdded,
        @Schema(description = "合并产生的行组分块数")
        int rowsMerged,
        @Schema(description = "去重移除的分块数")
        int deduplicated
) {
}
