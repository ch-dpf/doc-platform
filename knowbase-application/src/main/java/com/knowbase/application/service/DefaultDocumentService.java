package com.knowbase.application.service;

import com.knowbase.api.command.CreateIngestionRunCommand;
import com.knowbase.api.result.BatchDeleteDocumentsResult;
import com.knowbase.api.result.BatchObjectUploadResult;
import com.knowbase.api.result.DocumentUploadResult;
import com.knowbase.api.result.BatchReindexResult;
import com.knowbase.api.result.DocumentDuplicateGroupResult;
import com.knowbase.api.result.IngestionRunResult;
import com.knowbase.api.result.KnowledgeDocumentResult;
import com.knowbase.api.result.ObjectUploadResult;
import com.knowbase.api.result.PageResult;
import com.knowbase.application.mapper.DocumentChunkPresentation;
import com.knowbase.application.security.AccessControlService;
import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.model.KnowledgeDocument;
import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.domain.security.AclPermission;
import com.knowbase.domain.status.DocumentStatus;
import com.knowbase.application.usecase.RunIngestionUseCase;
import com.knowbase.ingestion.DocumentSourceLoader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class DefaultDocumentService {

    private final KnowbaseRepository repository;
    private final AccessControlService accessControlService;
    private final RunIngestionUseCase runIngestionUseCase;
    private final IndexGenerationService indexGenerationService;
    private final DefaultObjectUploadService uploadService;
    private final DocumentSourceLoader documentSourceLoader;

    public DefaultDocumentService(
            KnowbaseRepository repository,
            AccessControlService accessControlService,
            RunIngestionUseCase runIngestionUseCase,
            IndexGenerationService indexGenerationService,
            DefaultObjectUploadService uploadService,
            DocumentSourceLoader documentSourceLoader
    ) {
        this.repository = repository;
        this.accessControlService = accessControlService;
        this.runIngestionUseCase = runIngestionUseCase;
        this.indexGenerationService = indexGenerationService;
        this.uploadService = uploadService;
        this.documentSourceLoader = documentSourceLoader;
    }

    public List<KnowledgeDocumentResult> list(UUID libraryId, UUID indexVersionId) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.READ);
        repository.findLibrary(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("知识库不存在: " + libraryId));
        return repository.listDocuments(libraryId, indexVersionId).stream()
                .map(this::toResult)
                .toList();
    }

    public PageResult<KnowledgeDocumentResult> page(UUID libraryId, UUID indexVersionId, int page, int size) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.READ);
        repository.findLibrary(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("知识库不存在: " + libraryId));
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        var paged = repository.pageDocuments(libraryId, indexVersionId, safePage, safeSize);
        List<KnowledgeDocumentResult> items = paged.items().stream()
                .map(this::toResult)
                .toList();
        return new PageResult<>(items, paged.total(), safePage, safeSize);
    }

    public KnowledgeDocumentResult get(UUID libraryId, UUID documentId) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.READ);
        KnowledgeDocument document = requireDocument(libraryId, documentId);
        return toResult(document);
    }

    public DocumentPreviewContent preview(UUID libraryId, UUID documentId) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.READ);
        KnowledgeDocument document = requireDocument(libraryId, documentId);
        if (document.sourceUri() == null || document.sourceUri().isBlank()) {
            throw new IllegalStateException("文档缺少 sourceUri，无法预览: " + documentId);
        }
        DocumentSourceLoader.SourceContent source = documentSourceLoader.loadSourceContent(document.sourceUri());
        String filename = preferredFilename(document.title(), source.filename());
        return new DocumentPreviewContent(filename, source.mimeType(), source.content());
    }

    public void delete(UUID libraryId, UUID documentId) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.WRITE);
        KnowledgeDocument document = repository.findDocument(documentId)
                .filter(item -> item.libraryId().equals(libraryId))
                .orElseThrow(() -> new ResourceNotFoundException("文档不存在: " + documentId));
        UUID generationId = document.indexVersionId();
        repository.deleteDocumentAndChunks(documentId);
        indexGenerationService.refreshGenerationStats(generationId);
    }

    public BatchDeleteDocumentsResult deleteBatch(UUID libraryId, List<UUID> documentIds) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.WRITE);
        repository.findLibrary(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("知识库不存在: " + libraryId));
        if (documentIds == null || documentIds.isEmpty()) {
            throw new IllegalArgumentException("documentIds 不能为空");
        }
        Set<UUID> generationIds = new HashSet<>();
        List<UUID> deleted = new ArrayList<>();
        for (UUID documentId : documentIds) {
            repository.findDocument(documentId)
                    .filter(item -> item.libraryId().equals(libraryId))
                    .ifPresent(document -> {
                        repository.deleteDocumentAndChunks(documentId);
                        generationIds.add(document.indexVersionId());
                        deleted.add(documentId);
                    });
        }
        generationIds.forEach(indexGenerationService::refreshGenerationStats);
        return new BatchDeleteDocumentsResult(libraryId, deleted.size(), List.copyOf(deleted));
    }

    public IngestionRunResult reindex(UUID libraryId, UUID documentId) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.WRITE);
        KnowledgeDocument document = repository.findDocument(documentId)
                .filter(item -> item.libraryId().equals(libraryId))
                .orElseThrow(() -> new ResourceNotFoundException("文档不存在: " + documentId));
        if (document.sourceUri() == null || document.sourceUri().isBlank()) {
            throw new IllegalStateException("文档缺少 sourceUri，无法重索引: " + documentId);
        }
        return runIngestionUseCase.create(new CreateIngestionRunCommand(
                libraryId,
                List.of(document.sourceUri()),
                "reindex",
                null,
                Map.of()
        ));
    }

    public DocumentUploadResult uploadAndIngest(
            UUID libraryId,
            List<DefaultObjectUploadService.UploadCandidate> candidates,
            String documentProfileCode,
            boolean autoStart
    ) throws Exception {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.WRITE);
        repository.findLibrary(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("知识库不存在: " + libraryId));

        BatchObjectUploadResult uploadResult = uploadService.uploadBatch(candidates);
        IngestionRunResult ingestionRun = null;
        List<KnowledgeDocumentResult> documents = List.of();
        if (autoStart && !uploadResult.uploaded().isEmpty()) {
            List<String> sourceUris = uploadResult.uploaded().stream().map(ObjectUploadResult::uri).toList();
            ingestionRun = runIngestionUseCase.create(new CreateIngestionRunCommand(
                    libraryId,
                    sourceUris,
                    uploadService.storageType(),
                    documentProfileCode,
                    Map.of()
            ));
            UUID generationId = ingestionRun.indexVersionId() != null
                    ? ingestionRun.indexVersionId()
                    : indexGenerationService.ensureActiveGeneration(libraryId);
            documents = repository.listDocuments(libraryId, generationId).stream()
                    .filter(doc -> sourceUris.contains(doc.sourceUri()))
                    .map(this::toResult)
                    .toList();
        }
        return new DocumentUploadResult(uploadResult, ingestionRun, documents);
    }

    public BatchReindexResult reindexFailed(UUID libraryId) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.WRITE);
        UUID generationId = indexGenerationService.ensureActiveGeneration(libraryId);
        List<String> sourceUris = repository.listDocuments(libraryId, generationId).stream()
                .filter(doc -> doc.status() == DocumentStatus.FAILED)
                .map(KnowledgeDocument::sourceUri)
                .filter(uri -> uri != null && !uri.isBlank())
                .distinct()
                .toList();
        if (sourceUris.isEmpty()) {
            throw new IllegalStateException("没有 FAILED 状态的文档需要重索引");
        }
        IngestionRunResult run = runIngestionUseCase.create(new CreateIngestionRunCommand(
                libraryId,
                sourceUris,
                "reindex-failed",
                null,
                Map.of()
        ));
        return new BatchReindexResult(libraryId, sourceUris.size(), sourceUris, run);
    }

    public BatchReindexResult reindexByDocumentProfile(UUID libraryId, String documentProfileCode) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.WRITE);
        DocumentProfile profile = repository.findDocumentProfile(libraryId, documentProfileCode)
                .orElseThrow(() -> new ResourceNotFoundException("文档 Profile 不存在: " + documentProfileCode));
        UUID generationId = indexGenerationService.ensureActiveGeneration(libraryId);
        List<String> sourceUris = repository.listDocuments(libraryId, generationId).stream()
                .filter(doc -> profile.documentProfileId().equals(doc.documentProfileId()))
                .map(KnowledgeDocument::sourceUri)
                .filter(uri -> uri != null && !uri.isBlank())
                .distinct()
                .toList();
        if (sourceUris.isEmpty()) {
            throw new IllegalStateException("没有匹配 Profile「" + documentProfileCode + "」的文档");
        }
        IngestionRunResult run = runIngestionUseCase.create(new CreateIngestionRunCommand(
                libraryId,
                sourceUris,
                "reindex-by-profile",
                documentProfileCode,
                Map.of()
        ));
        return new BatchReindexResult(libraryId, sourceUris.size(), sourceUris, run);
    }

    public List<DocumentDuplicateGroupResult> listDuplicateGroups(UUID libraryId) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.READ);
        UUID generationId = indexGenerationService.ensureActiveGeneration(libraryId);
        Map<String, List<KnowledgeDocument>> grouped = repository.listDocuments(libraryId, generationId).stream()
                .filter(doc -> doc.contentHash() != null && !doc.contentHash().isBlank())
                .collect(Collectors.groupingBy(KnowledgeDocument::contentHash, LinkedHashMap::new, Collectors.toList()));
        List<DocumentDuplicateGroupResult> results = new ArrayList<>();
        for (Map.Entry<String, List<KnowledgeDocument>> entry : grouped.entrySet()) {
            if (entry.getValue().size() < 2) {
                continue;
            }
            results.add(new DocumentDuplicateGroupResult(
                    entry.getKey(),
                    entry.getValue().size(),
                    entry.getValue().stream().map(KnowledgeDocument::documentId).toList(),
                    entry.getValue().stream().map(KnowledgeDocument::sourceUri).toList()
            ));
        }
        return results;
    }

    private KnowledgeDocument requireDocument(UUID libraryId, UUID documentId) {
        return repository.findDocument(documentId)
                .filter(item -> item.libraryId().equals(libraryId))
                .orElseThrow(() -> new ResourceNotFoundException("文档不存在: " + documentId));
    }

    private static String preferredFilename(String title, String fallback) {
        if (title != null && !title.isBlank()) {
            return title;
        }
        return fallback;
    }

    private KnowledgeDocumentResult toResult(KnowledgeDocument document) {
        int chunkCount = DocumentChunkPresentation.excludeSummaryChunks(
                repository.listChunksByDocument(document.documentId())
        ).size();
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
}
