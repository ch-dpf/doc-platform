package com.knowbase.api.result;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "库类型预设产品说明")
public record LibraryTypePresetGuideResult(
        @Schema(description = "预设编码")
        String code,
        @Schema(description = "预设名称")
        String name,
        @Schema(description = "场景描述")
        String description,
        @Schema(description = "实例与模板关系说明")
        String instanceBindingNoteZh,
        @Schema(description = "适合上传的文件类型")
        List<String> suitableFileTypesZh,
        @Schema(description = "需谨慎或需额外 Profile 的文件类型")
        List<String> cautionFileTypesZh,
        @Schema(description = "L1 默认参数摘要")
        Map<String, Object> l1Defaults,
        @Schema(description = "本预设包含的 Document Profile 说明")
        List<DocumentProfileGuideResult> documentProfiles,
        @Schema(description = "库配置变更影响提示")
        List<String> changeImpactHintsZh
) {
}
