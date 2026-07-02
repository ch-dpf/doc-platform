package com.knowbase.application.service;

import com.knowbase.api.result.IndexVersionResult;
import com.knowbase.api.result.PromoteReadinessResult;
import com.knowbase.application.security.AccessControlService;
import com.knowbase.domain.model.IndexVersion;
import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.domain.security.AclPermission;

import java.util.UUID;

public final class DefaultIndexVersionService {

    private final KnowbaseRepository repository;
    private final AccessControlService accessControlService;
    private final IndexGenerationService indexGenerationService;
    private final DefaultLibraryIndexHealthService indexHealthService;

    public DefaultIndexVersionService(
            KnowbaseRepository repository,
            AccessControlService accessControlService,
            IndexGenerationService indexGenerationService,
            DefaultLibraryIndexHealthService indexHealthService
    ) {
        this.repository = repository;
        this.accessControlService = accessControlService;
        this.indexGenerationService = indexGenerationService;
        this.indexHealthService = indexHealthService;
    }

    public DefaultIndexVersionService(
            KnowbaseRepository repository,
            AccessControlService accessControlService,
            IndexGenerationService indexGenerationService
    ) {
        this(repository, accessControlService, indexGenerationService, new DefaultLibraryIndexHealthService(repository, indexGenerationService));
    }

    public IndexVersionResult publish(UUID libraryId, UUID indexVersionId, boolean force) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.WRITE);
        repository.findLibrary(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("知识库不存在: " + libraryId));
        if (!force) {
            PromoteReadinessResult readiness = indexHealthService.checkPromote(libraryId, indexVersionId);
            if (readiness.blocked()) {
                throw new IllegalStateException(String.join("；", readiness.blockers()));
            }
        }
        IndexVersion published = indexGenerationService.promoteGeneration(libraryId, indexVersionId);
        return toResult(published);
    }

    public IndexVersionResult publish(UUID libraryId, UUID indexVersionId) {
        return publish(libraryId, indexVersionId, false);
    }

    private static IndexVersionResult toResult(IndexVersion indexVersion) {
        return new IndexVersionResult(
                indexVersion.indexVersionId(),
                indexVersion.libraryId(),
                indexVersion.profileId(),
                indexVersion.version(),
                indexVersion.status().name(),
                indexVersion.documentCount(),
                indexVersion.chunkCount(),
                indexVersion.publishedAt(),
                indexVersion.createdAt()
        );
    }
}
