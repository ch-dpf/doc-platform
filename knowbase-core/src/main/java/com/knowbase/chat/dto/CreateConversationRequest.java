package com.knowbase.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateConversationRequest(
        @NotBlank String tenantId,
        String title
) {}
