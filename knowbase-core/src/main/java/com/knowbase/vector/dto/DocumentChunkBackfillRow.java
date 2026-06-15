package com.knowbase.vector.dto;

import java.util.UUID;

public record DocumentChunkBackfillRow(
        UUID chunkId, int chunkIndex, String content, String metadataJson) {}
