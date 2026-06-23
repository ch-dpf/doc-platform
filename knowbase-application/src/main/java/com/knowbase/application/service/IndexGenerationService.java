package com.knowbase.application.service;

import com.knowbase.domain.model.IndexVersion;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.domain.status.IndexVersionStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages index generations (internal index versions): active pointer, initial gen-1, stats refresh.
 */
public final class IndexGenerationService {

    private final KnowbaseRepository repository;

    public IndexGenerationService(KnowbaseRepository repository) {
        this.repository = repository;
    }

    public UUID ensureActiveGeneration(UUID libraryId) {
        Optional<IndexVersion> active = repository.findActiveIndexVersion(libraryId);
        if (active.isPresent()) {
            return active.get().indexVersionId();
        }
        LibraryProfile profile = repository.findLatestLibraryProfile(libraryId)
                .orElseThrow(() -> new IllegalStateException("知识库 Profile 不存在: " + libraryId));
        return createInitialGeneration(libraryId, profile.profileId()).indexVersionId();
    }

    public IndexVersion createInitialGeneration(UUID libraryId, UUID profileId) {
        UUID generationId = UUID.randomUUID();
        Instant now = Instant.now();
        IndexVersion generation = new IndexVersion(
                generationId,
                libraryId,
                profileId,
                1,
                IndexVersionStatus.PUBLISHED,
                0,
                0,
                now,
                now
        );
        repository.saveIndexVersion(generation);
        repository.setActiveIndexGeneration(libraryId, generationId);
        return generation;
    }

    public IndexVersion promoteGeneration(UUID libraryId, UUID indexVersionId) {
        IndexVersion target = repository.findIndexVersion(indexVersionId)
                .filter(item -> item.libraryId().equals(libraryId))
                .orElseThrow(() -> new ResourceNotFoundException("索引代次不存在: " + indexVersionId));
        if (target.status() == IndexVersionStatus.FAILED || target.status() == IndexVersionStatus.BUILDING) {
            throw new IllegalStateException("只能 promote 已构建完成的索引代次: " + target.status());
        }
        IndexVersion published = new IndexVersion(
                target.indexVersionId(),
                target.libraryId(),
                target.profileId(),
                target.version(),
                IndexVersionStatus.PUBLISHED,
                target.documentCount(),
                target.chunkCount(),
                Instant.now(),
                target.createdAt()
        );
        repository.saveIndexVersion(published);
        repository.archivePublishedGenerationsExcept(libraryId, indexVersionId);
        repository.reassignDocumentsToGeneration(libraryId, indexVersionId);
        repository.setActiveIndexGeneration(libraryId, indexVersionId);
        return published;
    }

    public IndexVersion createPendingGeneration(UUID libraryId) {
        LibraryProfile profile = repository.findLatestLibraryProfile(libraryId)
                .orElseThrow(() -> new IllegalStateException("知识库 Profile 不存在: " + libraryId));
        repository.findLibrary(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("知识库不存在: " + libraryId));
        int nextVersion = repository.listIndexVersions(libraryId).stream()
                .mapToInt(IndexVersion::version)
                .max()
                .orElse(0) + 1;
        UUID generationId = UUID.randomUUID();
        Instant now = Instant.now();
        IndexVersion generation = new IndexVersion(
                generationId,
                libraryId,
                profile.profileId(),
                nextVersion,
                IndexVersionStatus.BUILDING,
                0,
                0,
                null,
                now
        );
        repository.saveIndexVersion(generation);
        return generation;
    }

    public IndexVersion markGenerationReady(UUID indexVersionId, int documentCount, int chunkCount) {
        IndexVersion current = repository.findIndexVersion(indexVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("索引代次不存在: " + indexVersionId));
        IndexVersion ready = new IndexVersion(
                current.indexVersionId(),
                current.libraryId(),
                current.profileId(),
                current.version(),
                IndexVersionStatus.DRAFT,
                documentCount,
                chunkCount,
                null,
                current.createdAt()
        );
        repository.saveIndexVersion(ready);
        return ready;
    }

    public IndexVersion markGenerationFailed(UUID indexVersionId) {
        IndexVersion current = repository.findIndexVersion(indexVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("索引代次不存在: " + indexVersionId));
        IndexVersion failed = new IndexVersion(
                current.indexVersionId(),
                current.libraryId(),
                current.profileId(),
                current.version(),
                IndexVersionStatus.FAILED,
                current.documentCount(),
                current.chunkCount(),
                null,
                current.createdAt()
        );
        repository.saveIndexVersion(failed);
        return failed;
    }

    public void refreshGenerationStats(UUID indexVersionId) {
        repository.refreshIndexVersionStats(indexVersionId);
    }
}
