package com.knowbase.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

@Schema(description = "创建或更新 Tokenizer Profile 请求")
public record CreateTokenizerProfileCommand(
        @Schema(description = "模型提供方", example = "ollama")
        @NotBlank String provider,
        @Schema(description = "模型名称", example = "bge-m3")
        @NotBlank String modelName,
        @Schema(description = "Tokenizer 标识", example = "ollama:bge-m3")
        @NotBlank String tokenizerId,
        @Schema(description = "Tokenizer 版本", example = "bge-m3")
        @NotBlank String tokenizerVersion,
        @Schema(description = "是否为近似 tokenizer", example = "true")
        boolean approximate,
        @Schema(description = "Tokenizer 扩展配置")
        Map<String, Object> config,
        @Schema(description = "是否启用", example = "true")
        boolean enabled
) {
}
