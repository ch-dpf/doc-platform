package com.knowbase.vector.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record RebuildLibraryRequest(
        @NotNull UUID libraryId,
        @NotBlank String tenantId,
        @Schema(description = "可选：仅重索引该分块档下的文档；省略则全库已解析文档")
        @Size(max = 32)
        String chunkProfileId) {}
