package com.knowbase.api.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CreateAgentVersionCommand(
        @NotBlank String scenePresetCode,
        @NotNull List<UUID> libraryIds,
        Map<String, Object> routingPolicy,
        Map<String, Object> retrievalPolicy,
        Map<String, Object> answerPolicy,
        String systemPrompt,
        UUID chatTokenizerProfileId
) {
}
