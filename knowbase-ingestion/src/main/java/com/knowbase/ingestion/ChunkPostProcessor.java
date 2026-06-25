package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentChunk;

import java.util.List;

public interface ChunkPostProcessor {

    boolean supports(ChunkPostProcessContext context);

    List<DocumentChunk> process(List<DocumentChunk> chunks, ChunkPostProcessContext context);
}
