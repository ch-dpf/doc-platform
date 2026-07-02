package com.knowbase.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "知识库召回检索测试请求")
public record CreateLibraryRetrievalTestCommand(
        @NotBlank String question,
        Map<String, Object> retrievalPolicyOverride,
        Map<String, Object> answerPolicyOverride,
        @Schema(description = "可选：期望命中的 documentId，用于 Hit@K 判定") List<UUID> expectedDocumentIds,
        @Schema(description = "可选：期望命中的 sourceUri / 文件名") List<String> expectedSourceUris,
        @Schema(description = "可选：ground truth 文本片段（块内容包含即命中）") List<String> groundTruthContexts,
        @Schema(description = "Hit@K 的 K，默认 8") Integer hitRank
) {
    public CreateLibraryRetrievalTestCommand {
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

    public CreateLibraryRetrievalTestCommand(
            String question,
            Map<String, Object> retrievalPolicyOverride,
            Map<String, Object> answerPolicyOverride
    ) {
        this(question, retrievalPolicyOverride, answerPolicyOverride, List.of(), List.of(), List.of(), null);
    }
}
