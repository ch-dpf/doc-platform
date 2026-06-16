package com.knowbase.library.dto;

import com.knowbase.library.dto.config.LibraryParsingDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Schema(description = "更新解析配置（库配置 Tab：解析配置）")
public record UpdateLibraryParsingRequest(
        @Schema(description = "解析配置", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Valid
        LibraryParsingDto parsing) {}
