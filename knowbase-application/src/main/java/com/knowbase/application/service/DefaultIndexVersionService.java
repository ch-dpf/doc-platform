package com.knowbase.application.service;

import com.knowbase.api.result.IndexVersionResult;
import com.knowbase.application.security.AccessControlService;
import com.knowbase.domain.model.IndexVersion;
import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.domain.security.AclPermission;

import java.util.UUID;

public final class DefaultIndexVersionService {

    private final KnowbaseRepository repository;
    private final AccessControlService accessControlService;

    public DefaultIndexVersionService(KnowbaseRepository repository, AccessControlService accessControlService) {
        this.repository = repository;
        this.accessControlService = accessControlService;
    }

    public IndexVersionResult publish(UUID libraryId, UUID indexVersionId) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.WRITE);
        repository.findLibrary(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("知识库不存在: " + libraryId));
        IndexVersion published = repository.publishIndexVersion(indexVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("索引版本不存在: " + indexVersionId));
        if (!published.libraryId().equals(libraryId)) {
            throw new IllegalArgumentException("索引版本不属于当前知识库: " + indexVersionId);
        }
        return toResult(published);
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
