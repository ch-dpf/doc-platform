package com.knowbase.api.result;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Token 用量统计")
public record TokenUsageResult(
        @Schema(description = "提示词 Token 数")
        int promptTokens,
        @Schema(description = "生成内容 Token 数")
        int completionTokens,
        @Schema(description = "总 Token 数")
        int totalTokens,
        @Schema(description = "上下文证据 Token 数")
        int contextTokens,
        @Schema(description = "Tokenizer 标识")
        String tokenizerId,
        @Schema(description = "Tokenizer 版本")
        String tokenizerVersion
) {
}
