package com.knowbase.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "将指定分块档设为库主档（默认问答检索范围）")
public record SetPrimaryChunkProfileRequest(
        @NotBlank
        @Size(max = 32)
        @Schema(description = "分块档 ID", example = "cp_a1b2c3d4e5f6")
        String chunkProfileId) {}
