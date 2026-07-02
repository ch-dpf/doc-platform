package com.knowbase.application.service;

import com.knowbase.api.command.LibraryProfileCommand;
import com.knowbase.api.result.LibraryProfileResult;
import com.knowbase.api.result.LibraryProfileVersionResult;
import com.knowbase.application.security.AccessControlService;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.domain.security.AclPermission;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class DefaultLibraryProfileService {

    private final KnowbaseRepository repository;
    private final AccessControlService accessControlService;
    private final DefaultLibraryIndexHealthService indexHealthService;

    public DefaultLibraryProfileService(
            KnowbaseRepository repository,
            AccessControlService accessControlService,
            DefaultLibraryIndexHealthService indexHealthService
    ) {
        this.repository = repository;
        this.accessControlService = accessControlService;
        this.indexHealthService = indexHealthService;
    }

    public LibraryProfileResult getLatest(UUID libraryId) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.READ);
        repository.findLibrary(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("知识库不存在: " + libraryId));
        LibraryProfile profile = repository.findLatestLibraryProfile(libraryId)
                .orElseThrow(() -> new IllegalStateException("知识库 Profile 不存在: " + libraryId));
        return toResult(profile, indexHealthService.assess(libraryId));
    }

    public LibraryProfileResult getVersion(UUID libraryId, UUID profileId) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.READ);
        LibraryProfile profile = repository.findLibraryProfile(profileId)
                .filter(item -> item.libraryId().equals(libraryId))
                .orElseThrow(() -> new ResourceNotFoundException("Profile 版本不存在: " + profileId));
        return toResult(profile, indexHealthService.assess(libraryId));
    }

    public List<LibraryProfileVersionResult> listVersions(UUID libraryId) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.READ);
        List<LibraryProfile> profiles = repository.listLibraryProfiles(libraryId);
        if (profiles.isEmpty()) {
            return List.of();
        }
        LibraryProfile latest = profiles.getFirst();
        List<LibraryProfileVersionResult> versions = new ArrayList<>();
        for (int index = 0; index < profiles.size(); index++) {
            LibraryProfile profile = profiles.get(index);
            LibraryProfile previous = index + 1 < profiles.size() ? profiles.get(index + 1) : null;
            ProfileChange change = previous == null ? ProfileChange.none() : detectChange(previous, profile);
            versions.add(new LibraryProfileVersionResult(
                    profile.profileId(),
                    profile.libraryId(),
                    profile.version(),
                    profile.createdAt(),
                    change.l1Changed(),
                    change.l2Changed(),
                    change.changedFields(),
                    change.suggestedActions(latest.profileId().equals(profile.profileId()))
            ));
        }
        return versions;
    }

    public LibraryProfileResult createVersion(UUID libraryId, LibraryProfileCommand command) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.WRITE);
        repository.findLibrary(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("知识库不存在: " + libraryId));
        LibraryProfile current = repository.findLatestLibraryProfile(libraryId)
                .orElseThrow(() -> new IllegalStateException("知识库 Profile 不存在: " + libraryId));
        Instant now = Instant.now();
        Map<String, Object> mergedOptions = mergeOptions(current.options(), command.options());
        LibraryProfile next = new LibraryProfile(
                UUID.randomUUID(),
                libraryId,
                current.version() + 1,
                command.embeddingProvider(),
                command.embeddingModel(),
                command.embeddingDimension(),
                command.embeddingTokenizerProfileId(),
                command.chunkMaxTokens(),
                command.chunkOverlapTokens(),
                command.retrievalTopK(),
                mergedOptions,
                now
        );
        repository.saveLibraryProfile(next);
        return toResult(next, indexHealthService.assess(libraryId));
    }

    private static LibraryProfileResult toResult(LibraryProfile profile, com.knowbase.api.result.IndexHealthResult health) {
        return new LibraryProfileResult(
                profile.profileId(),
                profile.libraryId(),
                profile.version(),
                profile.embeddingProvider(),
                profile.embeddingModel(),
                profile.embeddingDimension(),
                profile.embeddingTokenizerProfileId(),
                profile.chunkMaxTokens(),
                profile.chunkOverlapTokens(),
                profile.retrievalTopK(),
                profile.options() == null ? Map.of() : profile.options(),
                profile.createdAt(),
                health.activeProfileId(),
                health.l1DriftDetected(),
                health.driftFields(),
                health.message()
        );
    }

    static ProfileChange detectChange(LibraryProfile previous, LibraryProfile next) {
        List<String> changed = new ArrayList<>();
        boolean l1 = false;
        boolean l2 = false;
        if (!Objects.equals(previous.embeddingProvider(), next.embeddingProvider())) {
            changed.add("embeddingProvider");
            l1 = true;
        }
        if (!Objects.equals(previous.embeddingModel(), next.embeddingModel())) {
            changed.add("embeddingModel");
            l1 = true;
        }
        if (previous.embeddingDimension() != next.embeddingDimension()) {
            changed.add("embeddingDimension");
            l1 = true;
        }
        if (!Objects.equals(previous.embeddingTokenizerProfileId(), next.embeddingTokenizerProfileId())) {
            changed.add("embeddingTokenizerProfileId");
            l1 = true;
        }
        if (previous.chunkMaxTokens() != next.chunkMaxTokens()) {
            changed.add("chunkMaxTokens");
            l2 = true;
        }
        if (previous.chunkOverlapTokens() != next.chunkOverlapTokens()) {
            changed.add("chunkOverlapTokens");
            l2 = true;
        }
        if (previous.retrievalTopK() != next.retrievalTopK()) {
            changed.add("retrievalTopK");
            l2 = true;
        }
        if (!Objects.equals(previous.options(), next.options())) {
            changed.add("options");
            l2 = true;
        }
        return new ProfileChange(l1, l2, List.copyOf(changed));
    }

    private static Map<String, Object> mergeOptions(Map<String, Object> current, Map<String, Object> override) {
        Map<String, Object> merged = new HashMap<>();
        if (current != null) {
            merged.putAll(current);
        }
        if (override != null) {
            merged.putAll(override);
        }
        return Map.copyOf(merged);
    }

    record ProfileChange(boolean l1Changed, boolean l2Changed, List<String> changedFields) {
        static ProfileChange none() {
            return new ProfileChange(false, false, List.of());
        }

        List<String> suggestedActions(boolean isLatest) {
            if (!isLatest) {
                return List.of();
            }
            List<String> actions = new ArrayList<>();
            if (l1Changed()) {
                actions.add("L1 变更：请执行全库 rebuild 并 promote 新索引代次");
            }
            if (l2Changed()) {
                actions.add("L2 变更：建议按 Document Profile 批量重索引，并重新跑 Recall@K 评测");
            }
            return List.copyOf(actions);
        }
    }
}
