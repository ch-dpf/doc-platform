package com.knowbase.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "批量导入召回评测黄金样本（JSON）")
public record ImportRetrievalEvalSamplesCommand(
        @Schema(description = "格式版本，默认 1") String version,
        @Schema(description = "为 true 时先清空本库已有样本") Boolean replaceExisting,
        @NotEmpty @Valid List<CreateRetrievalEvalSampleCommand> samples
) {
}
