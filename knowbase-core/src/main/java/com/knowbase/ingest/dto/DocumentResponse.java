package com.knowbase.ingest.dto;

import com.knowbase.ingest.domain.DocMetadata;
import com.knowbase.pipeline.config.IngestProfileSupport;
import com.knowbase.pipeline.config.IngestReport;
import com.knowbase.pipeline.content.ContentSignals;
import com.knowbase.pipeline.content.ContentSignalsSupport;
import com.knowbase.platform.JsonSupport;

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
        Instant updatedAt,
        IngestReport ingestReport,
        IngestProfileSummary ingestProfile,
        ContentSignals contentSignals,
        String chunkProfileId,
        boolean primaryProfile
) {
    public static DocumentResponse from(DocMetadata doc) {
        return from(doc, null, null, false);
    }

    public static DocumentResponse from(DocMetadata doc, Integer chunkCount) {
        return from(doc, chunkCount, null, false);
    }

    public static DocumentResponse from(
            DocMetadata doc, Integer chunkCount, String primaryChunkProfileId) {
        boolean primary = primaryChunkProfileId != null
                && !primaryChunkProfileId.isBlank()
                && primaryChunkProfileId.equals(doc.getChunkProfileId());
        return from(doc, chunkCount, doc.getChunkProfileId(), primary);
    }

    public static DocumentResponse from(
            DocMetadata doc, Integer chunkCount, String chunkProfileId, boolean primaryProfile) {
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
                doc.getUpdatedAt(),
                parseIngestReport(doc.getIngestReportJson()),
                IngestProfileSupport.toSummary(doc.getIngestProfileJson()),
                ContentSignalsSupport.parse(doc.getContentSignalsJson()),
                chunkProfileId != null ? chunkProfileId : doc.getChunkProfileId(),
                primaryProfile);
    }

    static IngestReport parseIngestReport(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return JsonSupport.fromJson(json, IngestReport.class);
        } catch (IllegalStateException ex) {
            return null;
        }
    }
}
