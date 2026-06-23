package com.knowbase.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "创建召回评测黄金样本")
public record CreateRetrievalEvalSampleCommand(
        @NotBlank String question,
        List<UUID> expectedDocumentIds,
        List<String> expectedSourceUris,
        List<String> groundTruthContexts,
        @Schema(description = "Hit@K 的 K，默认 8") Integer hitRank,
        String notes,
        Boolean enabled
) {
    public CreateRetrievalEvalSampleCommand {
        if (expectedDocumentIds == null) {
            expectedDocumentIds = List.of();
        }
        if (expectedSourceUris == null) {
            expectedSourceUris = List.of();
        }
        if (groundTruthContexts == null) {
            groundTruthContexts = List.of();
        }
    }
}
