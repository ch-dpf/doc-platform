package com.docplatform.ingest.dto;

import com.docplatform.ingest.domain.DocMetadata;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID docId,
        String tenantId,
        String sourceType,
        String sourceUrl,
        String fileName,
        String mimeType,
        long sizeBytes,
        String parseStatus,
        String indexStatus,
        int version,
        boolean indexRequested,
        Instant createdAt,
        Instant updatedAt
) {
    public static DocumentResponse from(DocMetadata doc) {
        return new DocumentResponse(
                doc.getDocId(),
                doc.getTenantId(),
                doc.getSourceType().name(),
                doc.getSourceUrl(),
                doc.getFileName(),
                doc.getMimeType(),
                doc.getSizeBytes(),
                doc.getParseStatus().name(),
                doc.getIndexStatus() != null ? doc.getIndexStatus().name() : null,
                doc.getVersion(),
                doc.isIndexRequested(),
                doc.getCreatedAt(),
                doc.getUpdatedAt());
    }
}
