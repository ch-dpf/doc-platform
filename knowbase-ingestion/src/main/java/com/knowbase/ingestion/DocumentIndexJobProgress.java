package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentIndexJob;
import com.knowbase.domain.model.KnowledgeDocument;
import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.domain.status.DocumentIndexJobStatus;
import com.knowbase.domain.status.DocumentStatus;

import java.time.Instant;
import java.util.UUID;

final class DocumentIndexJobProgress {

    private DocumentIndexJobProgress() {
    }

    static DocumentIndexJob start(
            KnowbaseRepository repository,
            UUID runId,
            UUID libraryId,
            String sourceUri
    ) {
        Instant now = Instant.now();
        DocumentIndexJob job = new DocumentIndexJob(
                UUID.randomUUID(),
                runId,
                libraryId,
                null,
                sourceUri,
                DocumentIndexJobStatus.RUNNING.name(),
                DocumentStatus.PARSING.name(),
                0,
                "开始解析文档",
                null,
                now,
                now
        );
        return repository.saveDocumentIndexJob(job);
    }

    static KnowledgeDocument advanceDocument(
            KnowbaseRepository repository,
            KnowledgeDocument document,
            DocumentStatus stage,
            String message
    ) {
        KnowledgeDocument updated = markStatus(document, stage, null);
        repository.saveDocument(updated);
        return updated;
    }

    static DocumentIndexJob advanceJob(
            KnowbaseRepository repository,
            DocumentIndexJob job,
            UUID documentId,
            DocumentStatus stage,
            String message
    ) {
        Instant now = Instant.now();
        DocumentIndexJob updated = new DocumentIndexJob(
                job.jobId(),
                job.runId(),
                job.libraryId(),
                documentId,
                job.sourceUri(),
                DocumentIndexJobStatus.RUNNING.name(),
                stage.name(),
                job.chunkCount(),
                message,
                null,
                job.createdAt(),
                now
        );
        return repository.saveDocumentIndexJob(updated);
    }

    static DocumentIndexJob succeed(
            KnowbaseRepository repository,
            DocumentIndexJob job,
            UUID documentId,
            int chunkCount
    ) {
        Instant now = Instant.now();
        DocumentIndexJob updated = new DocumentIndexJob(
                job.jobId(),
                job.runId(),
                job.libraryId(),
                documentId,
                job.sourceUri(),
                DocumentIndexJobStatus.SUCCEEDED.name(),
                DocumentStatus.INDEXED.name(),
                chunkCount,
                "文档已索引，共 " + chunkCount + " 个分块",
                null,
                job.createdAt(),
                now
        );
        return repository.saveDocumentIndexJob(updated);
    }

    static DocumentIndexJob fail(
            KnowbaseRepository repository,
            DocumentIndexJob job,
            UUID documentId,
            String errorMessage
    ) {
        Instant now = Instant.now();
        DocumentIndexJob updated = new DocumentIndexJob(
                job.jobId(),
                job.runId(),
                job.libraryId(),
                documentId,
                job.sourceUri(),
                DocumentIndexJobStatus.FAILED.name(),
                DocumentStatus.FAILED.name(),
                job.chunkCount(),
                "文档索引失败",
                errorMessage,
                job.createdAt(),
                now
        );
        return repository.saveDocumentIndexJob(updated);
    }

    private static KnowledgeDocument markStatus(KnowledgeDocument document, DocumentStatus status, String error) {
        Instant now = Instant.now();
        return new KnowledgeDocument(
                document.documentId(),
                document.libraryId(),
                document.indexVersionId(),
                document.sourceUri(),
                document.title(),
                status,
                document.documentProfileId(),
                document.contentHash(),
                document.lastIndexedAt(),
                error,
                document.createdAt(),
                now
        );
    }
}
