package com.knowbase.api.command;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "更新召回评测黄金样本")
public record UpdateRetrievalEvalSampleCommand(
        String question,
        List<UUID> expectedDocumentIds,
        List<String> expectedSourceUris,
        List<String> groundTruthContexts,
        Integer hitRank,
        String notes,
        Boolean enabled
) {
}
