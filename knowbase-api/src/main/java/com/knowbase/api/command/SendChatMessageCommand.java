package com.knowbase.api.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record SendChatMessageCommand(
        @NotBlank String content,
        UUID agentVersionId,
        List<UUID> debugLibraryIds
) {
}
