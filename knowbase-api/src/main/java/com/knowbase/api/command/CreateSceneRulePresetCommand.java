package com.knowbase.api.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record CreateSceneRulePresetCommand(
        @NotBlank String tenantId,
        @NotBlank String code,
        @NotBlank String name,
        String description,
        @NotNull Map<String, Object> config
) {
}
