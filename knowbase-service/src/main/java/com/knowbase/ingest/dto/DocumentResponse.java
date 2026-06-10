package com.knowbase.ingest.dto;

import com.knowbase.ingest.domain.DocMetadata;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID docId,
        UUID libraryId,
        String tenantId,
        String sourceType,
        String sourceUrl,
        String fileName,
        String mimeType,
        long sizeBytes,
        Integer chunkCount,
        String parseStatus,
        String indexStatus,
        int version,
        boolean indexRequested,
        Instant createdAt,
        Instant updatedAt
) {
    public static DocumentResponse from(DocMetadata doc) {
        return from(doc, null);
    }

    public static DocumentResponse from(DocMetadata doc, Integer chunkCount) {
        return new DocumentResponse(
                doc.getDocId(),
                doc.getLibraryId(),
                doc.getTenantId(),
                doc.getSourceType().name(),
                doc.getSourceUrl(),
                doc.getFileName(),
                doc.getMimeType(),
                doc.getSizeBytes(),
                chunkCount,
                doc.getParseStatus().name(),
                doc.getIndexStatus() != null ? doc.getIndexStatus().name() : null,
                doc.getVersion(),
                doc.isIndexRequested(),
                doc.getCreatedAt(),
                doc.getUpdatedAt());
    }
}
