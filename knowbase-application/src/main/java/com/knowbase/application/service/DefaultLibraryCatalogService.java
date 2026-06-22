package com.knowbase.application.service;

import com.knowbase.api.result.DocumentChunkResult;
import com.knowbase.api.result.IndexVersionResult;
import com.knowbase.api.result.IngestionDocumentErrorResult;
import com.knowbase.api.result.KnowledgeDocumentResult;
import com.knowbase.application.mapper.ResultMapper;
import com.knowbase.application.security.AccessControlService;
import com.knowbase.domain.model.DocumentChunk;
import com.knowbase.domain.model.IndexVersion;
import com.knowbase.domain.model.IngestionDocumentError;
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
                .map(DefaultLibraryCatalogService::toDocumentResult)
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

    public List<IngestionDocumentErrorResult> listIngestionErrors(UUID runId) {
        var run = repository.findIngestionRun(runId)
                .orElseThrow(() -> new ResourceNotFoundException("入库运行不存在: " + runId));
        accessControlService.requireLibraryAccess(run.libraryId(), AclPermission.READ);
        return repository.listIngestionDocumentErrors(runId).stream()
                .map(DefaultLibraryCatalogService::toErrorResult)
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

    private static KnowledgeDocumentResult toDocumentResult(KnowledgeDocument document) {
        return new KnowledgeDocumentResult(
                document.documentId(),
                document.libraryId(),
                document.indexVersionId(),
                document.sourceUri(),
                document.title(),
                document.createdAt()
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
}
