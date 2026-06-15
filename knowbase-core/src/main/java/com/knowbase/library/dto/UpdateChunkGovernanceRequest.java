package com.knowbase.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "分块档治理策略")
public record UpdateChunkGovernanceRequest(
        @Schema(description = "是否允许采集侧使用非主档分块覆盖") boolean allowCustomChunkProfiles,
        @Min(1)
        @Max(20)
        @Schema(description = "单库最大活跃分块档数量", example = "5")
        int maxActiveChunkProfiles) {}
