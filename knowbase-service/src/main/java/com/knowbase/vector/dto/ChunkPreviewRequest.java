package com.knowbase.vector.dto;



import jakarta.validation.constraints.Max;

import jakarta.validation.constraints.Min;

import jakarta.validation.constraints.NotBlank;



import java.util.UUID;



/**

 * Chunk preview request. When {@code libraryId} is present, chunk sizing fields are ignored

 * and resolved from the library configuration via {@code EffectiveConfigResolver}.

 * Splitter strategy follows MIME rules; numeric fields follow library or request overrides.

 */

public record ChunkPreviewRequest(

        @NotBlank String sampleText,

        @Min(100) @Max(8000) int chunkSize,

        @Min(0) @Max(2000) int chunkOverlap,

        @Min(20) @Max(2000) int minChunkSize,

        @Min(200) @Max(16000) int maxChunkSize,

        @Min(0) @Max(500) int minParagraphLength,

        UUID libraryId,

        String mimeType,

        String ingestProfileJson) {}

