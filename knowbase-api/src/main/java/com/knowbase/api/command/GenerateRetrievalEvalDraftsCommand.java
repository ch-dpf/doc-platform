package com.knowbase.api.command;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "根据已入库文档内容生成评测草稿（默认未启用）")
public record GenerateRetrievalEvalDraftsCommand(
        @Schema(description = "仅处理该入库运行中成功的文档")
        UUID ingestionRunId,
        @Schema(description = "指定文档 ID 列表；省略时结合 ingestionRunId 或全库 active 代次")
        List<UUID> documentIds,
        @Schema(description = "重新生成前删除这些文档已有的 auto-draft 样本")
        Boolean replaceExistingAutoDrafts
) {
    public GenerateRetrievalEvalDraftsCommand {
        if (documentIds == null) {
            documentIds = List.of();
        }
    }
}
