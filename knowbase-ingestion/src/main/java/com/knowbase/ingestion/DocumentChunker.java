package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentChunk;

import java.util.List;
import java.util.UUID;

public interface DocumentChunker {

    List<DocumentChunk> chunk(UUID libraryId, UUID documentId, ParsedDocument document);
}
