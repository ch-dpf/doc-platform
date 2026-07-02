package com.knowbase.application.service;

import com.knowbase.api.result.IndexHealthResult;
import com.knowbase.api.result.PromoteReadinessResult;
import com.knowbase.domain.model.IndexVersion;
import com.knowbase.domain.model.KnowledgeLibrary;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.domain.status.IndexVersionStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class DefaultLibraryIndexHealthService {

    private final KnowbaseRepository repository;
    private final IndexGenerationService indexGenerationService;
    private final DefaultPromoteEvalGateService promoteEvalGateService;

    public DefaultLibraryIndexHealthService(
            KnowbaseRepository repository,
            IndexGenerationService indexGenerationService
    ) {
        this(repository, indexGenerationService, null);
    }

    public DefaultLibraryIndexHealthService(
            KnowbaseRepository repository,
            IndexGenerationService indexGenerationService,
            DefaultPromoteEvalGateService promoteEvalGateService
    ) {
        this.repository = repository;
        this.indexGenerationService = indexGenerationService;
        this.promoteEvalGateService = promoteEvalGateService;
    }

    public IndexHealthResult assess(UUID libraryId) {
        repository.findLibrary(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("知识库不存在: " + libraryId));
        LibraryProfile latestProfile = repository.findLatestLibraryProfile(libraryId)
                .orElseThrow(() -> new IllegalStateException("知识库 Profile 不存在: " + libraryId));
        UUID activeGenerationId = indexGenerationService.ensureActiveGeneration(libraryId);
        IndexVersion activeGeneration = repository.findIndexVersion(activeGenerationId)
                .orElseThrow(() -> new ResourceNotFoundException("索引代次不存在: " + activeGenerationId));
        LibraryProfile activeProfile = repository.findLibraryProfile(activeGeneration.profileId())
                .orElse(latestProfile);

        List<String> driftFields = detectL1Drift(activeProfile, latestProfile);
        boolean drift = !driftFields.isEmpty();
        String message = drift
                ? "检测到 L1 索引不变量变更（" + String.join("、", driftFields) + "），建议全库 rebuild 并 promote 新代次"
                : "active 代次与当前 Library Profile 一致";

        return new IndexHealthResult(
                libraryId,
                activeGenerationId,
                activeProfile.profileId(),
                latestProfile.profileId(),
                drift,
                drift,
                List.copyOf(driftFields),
                message
        );
    }

    public PromoteReadinessResult checkPromote(UUID libraryId, UUID indexGenerationId) {
        repository.findLibrary(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("知识库不存在: " + libraryId));
        IndexVersion target = repository.findIndexVersion(indexGenerationId)
                .filter(item -> item.libraryId().equals(libraryId))
                .orElseThrow(() -> new ResourceNotFoundException("索引代次不存在: " + indexGenerationId));

        List<String> warnings = new ArrayList<>();
        List<String> blockers = new ArrayList<>();

        if (target.status() == IndexVersionStatus.FAILED) {
            blockers.add("索引代次状态为 FAILED，无法 promote");
        }
        if (target.status() == IndexVersionStatus.BUILDING) {
            blockers.add("索引代次仍在 BUILDING，请等待重建完成");
        }
        if (target.chunkCount() <= 0) {
            blockers.add("索引代次没有可检索的分块");
        }

        IndexHealthResult health = assess(libraryId);
        if (health.l1DriftDetected() && !Objects.equals(target.profileId(), health.latestProfileId())) {
            warnings.add("目标代次基于旧 Profile 构建，当前 Library Profile 已变更，promote 后检索向量空间可能不一致");
        }

        KnowledgeLibrary library = repository.findLibrary(libraryId).orElseThrow();
        if (library.activeIndexGenerationId() != null && library.activeIndexGenerationId().equals(indexGenerationId)) {
            warnings.add("该代次已是当前 active 代次");
        }

        if (promoteEvalGateService != null) {
            var evalGate = promoteEvalGateService.evaluate(libraryId);
            if (evalGate.enabled() && !evalGate.passed()) {
                blockers.addAll(evalGate.failures());
                if (evalGate.failures().isEmpty()) {
                    blockers.add("promote 评测门禁未通过");
                }
            }
        }

        boolean blocked = !blockers.isEmpty();
        boolean ready = !blocked && target.status() != IndexVersionStatus.PUBLISHED;
        return new PromoteReadinessResult(
                libraryId,
                indexGenerationId,
                ready,
                blocked,
                List.copyOf(warnings),
                List.copyOf(blockers)
        );
    }

    private static List<String> detectL1Drift(LibraryProfile active, LibraryProfile latest) {
        List<String> fields = new ArrayList<>();
        if (!Objects.equals(active.embeddingProvider(), latest.embeddingProvider())) {
            fields.add("embeddingProvider");
        }
        if (!Objects.equals(active.embeddingModel(), latest.embeddingModel())) {
            fields.add("embeddingModel");
        }
        if (active.embeddingDimension() != latest.embeddingDimension()) {
            fields.add("embeddingDimension");
        }
        if (!Objects.equals(active.embeddingTokenizerProfileId(), latest.embeddingTokenizerProfileId())) {
            fields.add("embeddingTokenizerProfileId");
        }
        return fields;
    }
}
