package com.knowbase.api.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GrantAclCommand(
        @NotBlank String tenantId,
        @NotBlank String resourceType,
        @NotNull UUID resourceId,
        @NotBlank String principalType,
        @NotBlank String principalId,
        @NotBlank String permission
) {
}
