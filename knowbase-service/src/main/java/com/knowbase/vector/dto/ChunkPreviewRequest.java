package com.knowbase.vector.dto;

import com.knowbase.library.config.CleaningRulesSettings;
import com.knowbase.library.config.TextNormalizationSettings;
import com.knowbase.vector.chunk.ChunkingStrategy;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Chunk preview request. When {@code libraryId} is present, chunk sizing fields are ignored
 * and resolved from the library configuration via {@code LibraryConfigResolver}.
 */
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
        TextNormalizationSettings textNormalization,
        CleaningRulesSettings cleaning,
        UUID libraryId) {}
