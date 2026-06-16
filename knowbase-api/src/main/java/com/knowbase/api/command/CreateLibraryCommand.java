package com.knowbase.api.command;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateLibraryCommand(
        String tenantId,
        @NotBlank String name,
        String description,
        List<String> tags) {

    public CreateLibraryCommand(String tenantId, String name, String description) {
        this(tenantId, name, description, List.of());
    }
}
