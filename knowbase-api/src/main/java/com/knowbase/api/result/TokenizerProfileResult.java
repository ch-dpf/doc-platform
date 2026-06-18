package com.knowbase.api.result;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Tokenizer Profile 信息")
public record TokenizerProfileResult(
        @Schema(description = "Tokenizer Profile ID")
        UUID tokenizerProfileId,
        @Schema(description = "模型提供方")
        String provider,
        @Schema(description = "模型名称")
        String modelName,
        @Schema(description = "Tokenizer 标识")
        String tokenizerId,
        @Schema(description = "Tokenizer 版本")
        String tokenizerVersion,
        @Schema(description = "是否为近似 tokenizer")
        boolean approximate,
        @Schema(description = "扩展配置")
        Map<String, Object> config,
        @Schema(description = "是否启用")
        boolean enabled,
        @Schema(description = "创建时间")
        Instant createdAt,
        @Schema(description = "更新时间")
        Instant updatedAt
) {
}
