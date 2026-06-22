package com.knowbase.ingestion.chunking;

import com.knowbase.ingestion.chunking.SmartDocumentChunker.ChunkingOptions;
import com.knowbase.ingestion.chunking.SmartDocumentChunker.DocumentChunk;
import com.knowbase.ingestion.document.ParsedDocument;

import java.util.List;

/**
 * Chunking boundary that turns cleaned parsed documents into retrieval chunks.
 */
public interface DocumentChunker {

    List<DocumentChunk> chunk(ParsedDocument document, ChunkingOptions options);

    default List<DocumentChunk> chunk(ParsedDocument document) {
        return chunk(document, ChunkingOptions.defaults());
    }
}
