package com.docplatform.vector.dto;

import com.docplatform.library.config.TextNormalizationSettings;
import com.docplatform.vector.chunk.ChunkingStrategy;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChunkPreviewRequest(
        @NotBlank String sampleText,
        @NotNull ChunkingStrategy chunkingStrategy,
        @Min(100) @Max(8000) int chunkSize,
        @Min(0) @Max(2000) int chunkOverlap,
        @Min(20) @Max(2000) int minChunkSize,
        @Min(200) @Max(16000) int maxChunkSize,
        @Min(0) @Max(500) int minParagraphLength,
        boolean normalizeBeforeChunk,
        boolean textNormalizationEnabled,
        TextNormalizationSettings textNormalization) {}
