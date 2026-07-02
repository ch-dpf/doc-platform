package com.knowbase.api.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateEvalRunCommand(
        @NotBlank String tenantId,
        UUID agentId,
        @NotBlank String evalType,
        @NotNull List<EvalSampleInput> samples
) {
    public record EvalSampleInput(
            @NotBlank String question,
            String expectedAnswer
    ) {
    }
}
