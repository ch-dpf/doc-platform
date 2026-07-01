package com.knowbase.application.service;

import com.knowbase.api.command.CreateIngestionRunCommand;
import com.knowbase.api.result.IndexGenerationRebuildResult;
import com.knowbase.api.result.IndexVersionResult;
import com.knowbase.api.result.IngestionRunResult;
import com.knowbase.application.security.AccessControlService;
import com.knowbase.application.usecase.RunIngestionUseCase;
import com.knowbase.domain.model.IndexVersion;
import com.knowbase.domain.model.KnowledgeDocument;
import com.knowbase.domain.model.KnowledgeLibrary;
import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.domain.security.AclPermission;
import com.knowbase.ingestion.IngestionPipelineOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DefaultIndexGenerationRebuildService {

    private final KnowbaseRepository repository;
    private final AccessControlService accessControlService;
    private final IndexGenerationService indexGenerationService;
    private final RunIngestionUseCase runIngestionUseCase;
    private final DefaultIndexVersionService indexVersionService;

    public DefaultIndexGenerationRebuildService(
            KnowbaseRepository repository,
            AccessControlService accessControlService,
            IndexGenerationService indexGenerationService,
            RunIngestionUseCase runIngestionUseCase,
            DefaultIndexVersionService indexVersionService
    ) {
        this.repository = repository;
        this.accessControlService = accessControlService;
        this.indexGenerationService = indexGenerationService;
        this.runIngestionUseCase = runIngestionUseCase;
        this.indexVersionService = indexVersionService;
    }

    public IndexGenerationRebuildResult rebuild(UUID libraryId, boolean autoPromote) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.ADMIN);
        KnowledgeLibrary library = repository.findLibrary(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("知识库不存在: " + libraryId));
        UUID previousActiveId = library.activeIndexGenerationId();
        if (previousActiveId == null) {
            previousActiveId = indexGenerationService.ensureActiveGeneration(libraryId);
        }

        List<KnowledgeDocument> documents = repository.listDocuments(libraryId, previousActiveId);
        List<String> sourceUris = documents.stream()
                .map(KnowledgeDocument::sourceUri)
                .filter(uri -> uri != null && !uri.isBlank())
                .distinct()
                .toList();
        if (sourceUris.isEmpty()) {
            throw new IllegalStateException("当前索引代次没有可重建的文档");
        }

        IndexVersion pending = indexGenerationService.createPendingGeneration(libraryId);
        Map<String, Object> options = new HashMap<>();
        options.put(IngestionPipelineOptions.TARGET_INDEX_GENERATION_ID, pending.indexVersionId().toString());
        options.put(IngestionPipelineOptions.DEFER_DOCUMENT_GENERATION_UPDATE, true);
        options.put(IngestionPipelineOptions.REBUILD, true);
        options.put(IngestionPipelineOptions.PUBLISH_INDEX_ON_SUCCESS, false);

        IngestionRunResult run = runIngestionUseCase.create(new CreateIngestionRunCommand(
                libraryId,
                sourceUris,
                "rebuild",
                null,
                Map.copyOf(options)
        ));

        IndexVersion generation;
        if (run.chunkCount() > 0) {
            generation = indexGenerationService.markGenerationReady(
                    pending.indexVersionId(),
                    run.succeededDocuments(),
                    run.chunkCount()
            );
        } else {
            generation = indexGenerationService.markGenerationFailed(pending.indexVersionId());
        }

        boolean promoted = false;
        if (autoPromote && isSuccessful(run.status())) {
            indexVersionService.publish(libraryId, pending.indexVersionId(), false);
            generation = repository.findIndexVersion(pending.indexVersionId()).orElse(generation);
            promoted = true;
        }

        return new IndexGenerationRebuildResult(
                toResult(generation),
                run,
                previousActiveId,
                promoted
        );
    }

    private static boolean isSuccessful(String status) {
        return "SUCCEEDED".equals(status) || "PARTIAL_FAILED".equals(status);
    }

    private static IndexVersionResult toResult(IndexVersion version) {
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
}
