package com.knowbase.api.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateChatSessionCommand(
        @NotBlank String tenantId,
        @NotNull UUID agentId,
        UUID agentVersionId,
        String title
) {
}
