package com.knowbase.api.result;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "召回评测黄金样本")
public record RetrievalEvalSampleResult(
        UUID sampleId,
        UUID libraryId,
        String question,
        List<UUID> expectedDocumentIds,
        List<String> expectedSourceUris,
        List<String> groundTruthContexts,
        int hitRank,
        String notes,
        boolean enabled,
        @Schema(description = "是否为入库时自动生成的草稿（默认未启用）")
        boolean autoDraft,
        Instant createdAt,
        Instant updatedAt
) {
}
