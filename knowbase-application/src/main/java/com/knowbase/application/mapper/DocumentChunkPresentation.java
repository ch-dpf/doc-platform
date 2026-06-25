package com.knowbase.application.mapper;

import com.knowbase.domain.model.DocumentChunk;

import java.util.List;

/**
 * Presentation rules for document chunk APIs exposed to the admin console.
 */
public final class DocumentChunkPresentation {

    private DocumentChunkPresentation() {
    }

    public static boolean isSummaryChunk(DocumentChunk chunk) {
        if (chunk == null) {
            return false;
        }
        if ("document_summary".equals(chunk.chunkBoundaryType())) {
            return true;
        }
        if (chunk.metadata() == null) {
            return false;
        }
        Object role = chunk.metadata().get("chunkRole");
        return role != null && "document_summary".equals(String.valueOf(role));
    }

    public static List<DocumentChunk> excludeSummaryChunks(List<DocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }
        return chunks.stream()
                .filter(chunk -> !isSummaryChunk(chunk))
                .toList();
    }

    public static List<DocumentChunk> page(List<DocumentChunk> chunks, int page, int size) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }
        int from = Math.max(0, (page - 1) * size);
        if (from >= chunks.size()) {
            return List.of();
        }
        int to = Math.min(from + size, chunks.size());
        return chunks.subList(from, to);
    }
}
