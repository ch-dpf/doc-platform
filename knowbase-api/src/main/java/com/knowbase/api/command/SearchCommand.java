package com.knowbase.api.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SearchCommand(
        @NotNull UUID libraryId,
        String tenantId,
        @NotBlank String query,
        int topK) {}
