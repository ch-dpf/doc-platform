package com.knowbase.api.command;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "启动库级批量召回评测")
public record CreateRetrievalEvalRunCommand(
        @Schema(description = "全局 Hit@K，省略时使用各样本 hitRank 的最大值")
        Integer hitK,
        Map<String, Object> retrievalPolicyOverride,
        @Schema(description = "仅评测 enabled 样本，默认 true")
        Boolean enabledOnly
) {
}
