package com.knowbase.application.service;

import com.knowbase.api.result.DocumentChunkResult;
import com.knowbase.api.result.DocumentIndexJobResult;
import com.knowbase.api.result.IndexVersionResult;
import com.knowbase.api.result.IngestionDocumentErrorResult;
import com.knowbase.api.result.IngestionRunResult;
import com.knowbase.api.result.KnowledgeDocumentResult;
import com.knowbase.api.result.PageResult;
import com.knowbase.application.mapper.ResultMapper;
import com.knowbase.application.security.AccessControlService;
import com.knowbase.domain.model.DocumentChunk;
import com.knowbase.domain.model.DocumentIndexJob;
import com.knowbase.domain.model.IndexVersion;
import com.knowbase.domain.model.IngestionDocumentError;
import com.knowbase.domain.model.IngestionRun;
import com.knowbase.domain.model.KnowledgeDocument;
import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.domain.security.AclPermission;

import java.util.List;
import java.util.UUID;

public final class DefaultLibraryCatalogService {

    private final KnowbaseRepository repository;
    private final AccessControlService accessControlService;

    public DefaultLibraryCatalogService(KnowbaseRepository repository, AccessControlService accessControlService) {
        this.repository = repository;
        this.accessControlService = accessControlService;
    }

    public List<IndexVersionResult> listIndexVersions(UUID libraryId) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.READ);
        repository.findLibrary(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("知识库不存在: " + libraryId));
        return repository.listIndexVersions(libraryId).stream()
                .map(DefaultLibraryCatalogService::toIndexVersionResult)
                .toList();
    }

    public IndexVersionResult getIndexVersion(UUID libraryId, UUID indexVersionId) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.READ);
        IndexVersion version = repository.findIndexVersion(indexVersionId)
                .filter(item -> item.libraryId().equals(libraryId))
                .orElseThrow(() -> new ResourceNotFoundException("索引版本不存在: " + indexVersionId));
        return toIndexVersionResult(version);
    }

    public List<KnowledgeDocumentResult> listDocuments(UUID libraryId, UUID indexVersionId) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.READ);
        return repository.listDocuments(libraryId, indexVersionId).stream()
                .map(this::toDocumentResult)
                .toList();
    }

    public KnowledgeDocumentResult getDocument(UUID libraryId, UUID documentId) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.READ);
        KnowledgeDocument document = repository.findDocument(documentId)
                .filter(item -> item.libraryId().equals(libraryId))
                .orElseThrow(() -> new ResourceNotFoundException("文档不存在: " + documentId));
        return toDocumentResult(document);
    }

    public List<DocumentChunkResult> listDocumentChunks(UUID libraryId, UUID documentId) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.READ);
        repository.findDocument(documentId)
                .filter(item -> item.libraryId().equals(libraryId))
                .orElseThrow(() -> new ResourceNotFoundException("文档不存在: " + documentId));
        return repository.listChunksByDocument(documentId).stream()
                .map(DefaultLibraryCatalogService::toChunkResult)
                .toList();
    }

    public PageResult<DocumentChunkResult> pageDocumentChunks(UUID libraryId, UUID documentId, int page, int size) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.READ);
        repository.findDocument(documentId)
                .filter(item -> item.libraryId().equals(libraryId))
                .orElseThrow(() -> new ResourceNotFoundException("文档不存在: " + documentId));
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        var paged = repository.pageChunksByDocument(documentId, safePage, safeSize);
        List<DocumentChunkResult> items = paged.items().stream()
                .map(DefaultLibraryCatalogService::toChunkResult)
                .toList();
        return new PageResult<>(items, paged.total(), safePage, safeSize);
    }

    public List<IngestionDocumentErrorResult> listIngestionErrors(UUID runId) {
        var run = repository.findIngestionRun(runId)
                .orElseThrow(() -> new ResourceNotFoundException("入库运行不存在: " + runId));
        accessControlService.requireLibraryAccess(run.libraryId(), AclPermission.READ);
        return repository.listIngestionDocumentErrors(runId).stream()
                .map(DefaultLibraryCatalogService::toErrorResult)
                .toList();
    }

    public List<IngestionRunResult> listIngestionRuns(UUID libraryId, int limit) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.READ);
        repository.findLibrary(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("知识库不存在: " + libraryId));
        return repository.listIngestionRuns(libraryId, limit).stream()
                .map(ResultMapper::toIngestionRunResult)
                .toList();
    }

    public List<DocumentIndexJobResult> listDocumentIndexJobs(UUID libraryId, UUID runId) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.READ);
        IngestionRun run = repository.findIngestionRun(runId)
                .filter(item -> item.libraryId().equals(libraryId))
                .orElseThrow(() -> new ResourceNotFoundException("入库运行不存在: " + runId));
        return repository.listDocumentIndexJobs(run.runId()).stream()
                .map(DefaultLibraryCatalogService::toDocumentIndexJobResult)
                .toList();
    }

    private static IndexVersionResult toIndexVersionResult(IndexVersion version) {
        return new IndexVersionResult(
                version.indexVersionId(),
                version.libraryId(),
                version.profileId(),
                version.version(),
                version.status().name(),
                version.documentCount(),
                version.chunkCount(),
                version.publishedAt(),
                version.createdAt()
        );
    }

    private KnowledgeDocumentResult toDocumentResult(KnowledgeDocument document) {
        int chunkCount = repository.listChunksByDocument(document.documentId()).size();
        return new KnowledgeDocumentResult(
                document.documentId(),
                document.libraryId(),
                document.indexVersionId(),
                document.sourceUri(),
                document.title(),
                document.status().name(),
                chunkCount,
                document.lastIndexedAt(),
                document.lastError(),
                document.createdAt(),
                document.updatedAt()
        );
    }

    private static DocumentChunkResult toChunkResult(DocumentChunk chunk) {
        return new DocumentChunkResult(
                chunk.chunkId(),
                chunk.documentId(),
                chunk.libraryId(),
                chunk.indexVersionId(),
                chunk.content(),
                chunk.tokenCount(),
                chunk.tokenizerId(),
                chunk.tokenizerVersion(),
                chunk.embeddingModel(),
                chunk.chunkBoundaryType(),
                chunk.parentChunkId(),
                chunk.metadata()
        );
    }

    private static IngestionDocumentErrorResult toErrorResult(IngestionDocumentError error) {
        return new IngestionDocumentErrorResult(
                error.errorId(),
                error.runId(),
                error.sourceUri(),
                error.errorCode(),
                error.errorMessage(),
                error.createdAt()
        );
    }

    private static DocumentIndexJobResult toDocumentIndexJobResult(DocumentIndexJob job) {
        return new DocumentIndexJobResult(
                job.jobId(),
                job.runId(),
                job.libraryId(),
                job.documentId(),
                job.sourceUri(),
                job.status(),
                job.stage(),
                job.chunkCount(),
                job.message(),
                job.errorMessage(),
                job.createdAt(),
                job.updatedAt()
        );
    }
}
