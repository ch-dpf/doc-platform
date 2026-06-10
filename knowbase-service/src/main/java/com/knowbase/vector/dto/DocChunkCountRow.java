package com.knowbase.vector.dto;

import java.util.UUID;

public record DocChunkCountRow(UUID docId, int version, int chunkCount) {}
