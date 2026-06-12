package com.knowbase.library.dto;

import jakarta.validation.constraints.NotBlank;

public record MigrateToPrimaryRequest(@NotBlank String tenantId) {}
