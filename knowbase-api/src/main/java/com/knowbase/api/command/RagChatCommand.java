package com.knowbase.api.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RagChatCommand(
        @NotNull UUID libraryId,
        String tenantId,
        @NotBlank String question,
        Integer topK) {

    public RagChatCommand(UUID libraryId, String tenantId, String question) {
        this(libraryId, tenantId, question, null);
    }
}
