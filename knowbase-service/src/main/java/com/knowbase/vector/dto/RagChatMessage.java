package com.knowbase.vector.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 多轮对话中的一条消息（不含 system）。
 */
public record RagChatMessage(
        @NotBlank @Pattern(regexp = "user|assistant") String role,
        @NotBlank String content) {
}
