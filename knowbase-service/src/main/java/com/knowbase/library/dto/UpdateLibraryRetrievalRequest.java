package com.knowbase.library.dto;

import com.knowbase.library.config.RetrievalRulesSettings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Schema(description = "更新检索配置（库配置 Tab：检索）")
public record UpdateLibraryRetrievalRequest(
        @Schema(description = "检索规则配置体", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Valid
        RetrievalRulesSettings retrieval) {}
