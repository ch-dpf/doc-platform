package com.knowbase.application.service;

import com.knowbase.api.command.GenerateRetrievalEvalDraftsCommand;
import com.knowbase.api.command.CreateLibraryRetrievalTestCommand;
import com.knowbase.api.command.CreateRetrievalEvalRunCommand;
import com.knowbase.api.command.CreateRetrievalEvalSampleCommand;
import com.knowbase.api.command.ImportRetrievalEvalSamplesCommand;
import com.knowbase.api.result.RetrievalEvalBaselineResult;
import com.knowbase.api.command.UpdateRetrievalEvalSampleCommand;
import com.knowbase.api.result.RetrievalEvalResultItem;
import com.knowbase.api.result.RetrievalEvalRunResult;
import com.knowbase.api.result.RetrievalEvalSampleResult;
import com.knowbase.api.result.RetrievalHitCheckResult;
import com.knowbase.application.mapper.ResultMapper;
import com.knowbase.application.security.AccessControlService;
import com.knowbase.domain.model.KnowledgeLibrary;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.domain.model.RetrievalEvalBaseline;
import com.knowbase.domain.model.RetrievalEvalResult;
import com.knowbase.domain.model.RetrievalEvalRun;
import com.knowbase.domain.model.RetrievalEvalSample;
import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.domain.security.AclPermission;
import com.knowbase.domain.status.RetrievalEvalRunStatus;
import com.knowbase.retrieval.RetrievalCandidate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class DefaultRetrievalEvalService {

    private static final int DEFAULT_HIT_RANK = 8;

    private final KnowbaseRepository repository;
    private final AccessControlService accessControlService;
    private final DefaultLibraryRetrievalTestService libraryRetrievalTestService;
    private final RetrievalHitEvaluator hitEvaluator;
    private final IngestionEvalDraftService evalDraftService;

    public DefaultRetrievalEvalService(
            KnowbaseRepository repository,
            AccessControlService accessControlService,
            DefaultLibraryRetrievalTestService libraryRetrievalTestService,
            RetrievalHitEvaluator hitEvaluator,
            IngestionEvalDraftService evalDraftService
    ) {
        this.repository = repository;
        this.accessControlService = accessControlService;
        this.libraryRetrievalTestService = libraryRetrievalTestService;
        this.hitEvaluator = hitEvaluator;
        this.evalDraftService = evalDraftService;
    }

    public List<RetrievalEvalSampleResult> listSamples(UUID libraryId, boolean enabledOnly) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.READ);
        requireLibrary(libraryId);
        return repository.listRetrievalEvalSamples(libraryId, enabledOnly).stream()
                .map(ResultMapper::toRetrievalEvalSampleResult)
                .toList();
    }

    public RetrievalEvalSampleResult createSample(UUID libraryId, CreateRetrievalEvalSampleCommand command) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.WRITE);
        requireLibrary(libraryId);
        validateCriteria(command.expectedDocumentIds(), command.expectedSourceUris(), command.groundTruthContexts());
        Instant now = Instant.now();
        int hitRank = command.hitRank() == null ? DEFAULT_HIT_RANK : Math.max(1, command.hitRank());
        RetrievalEvalSample sample = new RetrievalEvalSample(
                UUID.randomUUID(),
                libraryId,
                command.question().trim(),
                List.copyOf(command.expectedDocumentIds()),
                List.copyOf(command.expectedSourceUris()),
                List.copyOf(command.groundTruthContexts()),
                hitRank,
                command.notes(),
                command.enabled() == null || command.enabled(),
                now,
                now
        );
        return ResultMapper.toRetrievalEvalSampleResult(repository.saveRetrievalEvalSample(sample));
    }

    public RetrievalEvalSampleResult updateSample(UUID libraryId, UUID sampleId, UpdateRetrievalEvalSampleCommand command) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.WRITE);
        RetrievalEvalSample existing = requireSample(libraryId, sampleId);
        List<UUID> expectedDocumentIds = command.expectedDocumentIds() == null
                ? existing.expectedDocumentIds()
                : List.copyOf(command.expectedDocumentIds());
        List<String> expectedSourceUris = command.expectedSourceUris() == null
                ? existing.expectedSourceUris()
                : List.copyOf(command.expectedSourceUris());
        List<String> groundTruthContexts = command.groundTruthContexts() == null
                ? existing.groundTruthContexts()
                : List.copyOf(command.groundTruthContexts());
        validateCriteria(expectedDocumentIds, expectedSourceUris, groundTruthContexts);
        RetrievalEvalSample updated = new RetrievalEvalSample(
                existing.sampleId(),
                existing.libraryId(),
                command.question() == null ? existing.question() : command.question().trim(),
                expectedDocumentIds,
                expectedSourceUris,
                groundTruthContexts,
                command.hitRank() == null ? existing.hitRank() : Math.max(1, command.hitRank()),
                command.notes() == null ? existing.notes() : command.notes(),
                command.enabled() == null ? existing.enabled() : command.enabled(),
                existing.createdAt(),
                Instant.now()
        );
        return ResultMapper.toRetrievalEvalSampleResult(repository.saveRetrievalEvalSample(updated));
    }

    public void deleteSample(UUID libraryId, UUID sampleId) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.WRITE);
        requireSample(libraryId, sampleId);
        repository.deleteRetrievalEvalSample(sampleId);
    }

    public RetrievalHitCheckResult evaluateHit(
            UUID libraryId,
            CreateLibraryRetrievalTestCommand command,
            List<RetrievalCandidate> rankedCandidates
    ) {
        validateCriteria(command.expectedDocumentIds(), command.expectedSourceUris(), command.groundTruthContexts());
        int hitRank = command.hitRank() == null ? DEFAULT_HIT_RANK : Math.max(1, command.hitRank());
        RetrievalEvalSample probe = new RetrievalEvalSample(
                UUID.randomUUID(),
                libraryId,
                command.question(),
                command.expectedDocumentIds(),
                command.expectedSourceUris(),
                command.groundTruthContexts(),
                hitRank,
                null,
                true,
                Instant.now(),
                Instant.now()
        );
        return toHitCheckResult(hitEvaluator.evaluate(probe, rankedCandidates));
    }

    public RetrievalEvalRunResult runEvaluation(UUID libraryId, CreateRetrievalEvalRunCommand command) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.READ);
        requireLibrary(libraryId);
        boolean enabledOnly = command.enabledOnly() == null || command.enabledOnly();
        List<RetrievalEvalSample> samples = repository.listRetrievalEvalSamples(libraryId, enabledOnly);
        Instant startedAt = Instant.now();
        Map<String, Object> retrievalPolicy = libraryRetrievalTestService.buildRetrievalPolicyForLibrary(
                libraryId,
                command.retrievalPolicyOverride()
        );
        int hitK = resolveHitK(command.hitK(), samples);
        UUID evalRunId = UUID.randomUUID();
        RetrievalEvalRun running = new RetrievalEvalRun(
                evalRunId,
                libraryId,
                RetrievalEvalRunStatus.RUNNING,
                hitK,
                samples.size(),
                0,
                null,
                retrievalPolicy,
                null,
                startedAt,
                null
        );
        repository.saveRetrievalEvalRun(running);
        if (samples.isEmpty()) {
            return completeRun(running, List.of(), "无可用黄金样本");
        }
        List<RetrievalEvalResultItem> items = new ArrayList<>();
        int passed = 0;
        List<RetrievalEvalMetricsAggregator.SampleMetrics> sampleMetrics = new ArrayList<>();
        try {
            for (RetrievalEvalSample sample : samples) {
                int sampleHitK = command.hitK() == null ? sample.hitRank() : hitK;
                RetrievalEvalSample effectiveSample = new RetrievalEvalSample(
                        sample.sampleId(),
                        sample.libraryId(),
                        sample.question(),
                        sample.expectedDocumentIds(),
                        sample.expectedSourceUris(),
                        sample.groundTruthContexts(),
                        sampleHitK,
                        sample.notes(),
                        sample.enabled(),
                        sample.createdAt(),
                        sample.updatedAt()
                );
                List<RetrievalCandidate> candidates = libraryRetrievalTestService.retrieveRankedCandidates(
                        libraryId,
                        new CreateLibraryRetrievalTestCommand(sample.question(), command.retrievalPolicyOverride(), null)
                );
                RetrievalHitEvaluator.HitEvaluation evaluation = hitEvaluator.evaluate(effectiveSample, candidates);
                if (evaluation.hit()) {
                    passed++;
                }
                double contextPrecision = hitEvaluator.contextPrecisionAtK(effectiveSample, candidates);
                String contentFamily = RetrievalEvalMetricsAggregator.resolveContentFamily(
                        sample,
                        evaluation,
                        candidates
                );
                sampleMetrics.add(new RetrievalEvalMetricsAggregator.SampleMetrics(
                        evaluation.hit(),
                        evaluation.firstHitRank(),
                        contextPrecision,
                        contentFamily
                ));
                Map<String, Object> trace = new HashMap<>();
                trace.put("retrievalPolicy", retrievalPolicy);
                trace.put("candidateCount", candidates.size());
                trace.put("contextPrecisionAtK", contextPrecision);
                trace.put("contentFamily", contentFamily);
                trace.put("explain", RetrievalExplainBuilder.buildRankedExplain(candidates, evaluation.hitRankUsed()));
                RetrievalEvalResult stored = repository.saveRetrievalEvalResult(new RetrievalEvalResult(
                        UUID.randomUUID(),
                        evalRunId,
                        sample.sampleId(),
                        sample.question(),
                        evaluation.hit(),
                        evaluation.hitRankUsed(),
                        evaluation.firstHitRank(),
                        evaluation.matchedDocumentId(),
                        evaluation.matchedChunkId(),
                        evaluation.matchType(),
                        evaluation.retrievedCount(),
                        evaluation.failureReason(),
                        Map.copyOf(trace),
                        Instant.now()
                ));
                items.add(ResultMapper.toRetrievalEvalResultItem(stored));
            }
            double recallAtK = samples.isEmpty() ? 0.0 : (double) passed / samples.size();
            double mrr = RetrievalEvalMetricsAggregator.mrr(sampleMetrics);
            double contextPrecisionAtK = RetrievalEvalMetricsAggregator.averageContextPrecision(sampleMetrics);
            Map<String, Double> stratifiedRecall = RetrievalEvalMetricsAggregator.stratifiedRecall(sampleMetrics);
            RetrievalEvalRun completed = new RetrievalEvalRun(
                    evalRunId,
                    libraryId,
                    RetrievalEvalRunStatus.SUCCEEDED,
                    hitK,
                    samples.size(),
                    passed,
                    recallAtK,
                    mrr,
                    contextPrecisionAtK,
                    stratifiedRecall,
                    retrievalPolicy,
                    "Recall@" + hitK + " = " + formatPercent(recallAtK)
                            + " · MRR = " + formatPercent(mrr)
                            + " · CP@" + hitK + " = " + formatPercent(contextPrecisionAtK),
                    startedAt,
                    Instant.now()
            );
            repository.saveRetrievalEvalRun(completed);
            return ResultMapper.toRetrievalEvalRunResult(completed, items);
        } catch (RuntimeException exception) {
            RetrievalEvalRun failed = new RetrievalEvalRun(
                    evalRunId,
                    libraryId,
                    RetrievalEvalRunStatus.FAILED,
                    hitK,
                    samples.size(),
                    passed,
                    null,
                    retrievalPolicy,
                    exception.getMessage(),
                    startedAt,
                    Instant.now()
            );
            repository.saveRetrievalEvalRun(failed);
            throw exception;
        }
    }

    public RetrievalEvalRunResult getEvaluation(UUID libraryId, UUID evalRunId) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.READ);
        RetrievalEvalRun evalRun = repository.findRetrievalEvalRun(evalRunId)
                .filter(run -> run.libraryId().equals(libraryId))
                .orElseThrow(() -> new ResourceNotFoundException("评测运行不存在: " + evalRunId));
        List<RetrievalEvalResultItem> items = repository.listRetrievalEvalResults(evalRunId).stream()
                .map(ResultMapper::toRetrievalEvalResultItem)
                .toList();
        return ResultMapper.toRetrievalEvalRunResult(evalRun, items);
    }

    public List<RetrievalEvalRunResult> listEvaluations(UUID libraryId, int limit) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.READ);
        requireLibrary(libraryId);
        return repository.listRetrievalEvalRuns(libraryId, Math.max(1, limit)).stream()
                .map(run -> ResultMapper.toRetrievalEvalRunResult(run, List.of()))
                .toList();
    }

    public PromoteEvalSummary summarizeForPromoteGate(UUID libraryId) {
        LibraryProfile profile = repository.findLatestLibraryProfile(libraryId)
                .orElseThrow(() -> new IllegalStateException("知识库 Profile 不存在: " + libraryId));
        List<RetrievalEvalSample> samples = repository.listRetrievalEvalSamples(libraryId, true);
        if (samples.isEmpty()) {
            return PromoteEvalSummary.notConfigured("未配置 kb_retrieval_eval_sample 黄金样本");
        }
        double threshold = readRecallThreshold(profile);
        double regressionDeltaMax = readRegressionDeltaMax(profile);
        RetrievalEvalRunResult result = runEvaluation(libraryId, new CreateRetrievalEvalRunCommand(null, null, true));
        double recall = result.recallAtK() == null ? 0.0 : result.recallAtK();
        Optional<RetrievalEvalBaseline> baseline = repository.findRetrievalEvalBaseline(libraryId);
        double baselineRecall = baseline.map(RetrievalEvalBaseline::recallAtK).orElse(Double.NaN);
        double regressionDelta = baseline.isPresent() ? baselineRecall - recall : 0.0;

        boolean absolutePass = recall >= threshold;
        boolean regressionPass = baseline.isEmpty() || recall >= baselineRecall - regressionDeltaMax;
        boolean passed = absolutePass && regressionPass;

        if (passed) {
            maybeRefreshBaseline(libraryId, profile, result, baseline.orElse(null));
            baseline = repository.findRetrievalEvalBaseline(libraryId);
            baselineRecall = baseline.map(RetrievalEvalBaseline::recallAtK).orElse(recall);
        }

        List<String> messages = new ArrayList<>();
        messages.add(String.format(
                "Recall@%d = %.1f%%（%d / %d），绝对阈值 %.1f%%",
                result.hitK(),
                recall * 100,
                result.passedSamples(),
                result.totalSamples(),
                threshold * 100
        ));
        if (baseline.isPresent()) {
            messages.add(String.format(
                    "基线 Recall@%d = %.1f%%，允许回落 %.1f%%",
                    baseline.get().hitK(),
                    baselineRecall * 100,
                    regressionDeltaMax * 100
            ));
        } else {
            messages.add("尚无回归基线，本次通过后将自动记录基线");
        }

        List<String> failures = new ArrayList<>();
        if (!absolutePass) {
            failures.add(String.format("Recall@%d %.1f%% 低于绝对阈值 %.1f%%", result.hitK(), recall * 100, threshold * 100));
        }
        if (!regressionPass) {
            failures.add(String.format(
                    "Recall@%d 相对基线回落 %.1f%%，超过允许 %.1f%%",
                    result.hitK(),
                    regressionDelta * 100,
                    regressionDeltaMax * 100
            ));
        }
        if (!passed) {
            for (RetrievalEvalResultItem item : result.results()) {
                if (!item.hit()) {
                    failures.add("未命中: " + abbreviate(item.question()) + " — " + item.failureReason());
                }
            }
        }
        return new PromoteEvalSummary(
                true,
                passed,
                threshold,
                regressionDeltaMax,
                recall,
                baselineRecall,
                regressionDelta,
                baseline.map(RetrievalEvalBaseline::evalRunId).orElse(null),
                result,
                List.copyOf(failures),
                List.copyOf(messages)
        );
    }

    public RetrievalEvalBaselineResult getBaseline(UUID libraryId) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.READ);
        requireLibrary(libraryId);
        return repository.findRetrievalEvalBaseline(libraryId)
                .map(ResultMapper::toRetrievalEvalBaselineResult)
                .orElse(null);
    }

    public RetrievalEvalBaselineResult pinBaseline(UUID libraryId, UUID evalRunId) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.WRITE);
        requireLibrary(libraryId);
        RetrievalEvalRun evalRun = repository.findRetrievalEvalRun(evalRunId)
                .filter(run -> run.libraryId().equals(libraryId))
                .orElseThrow(() -> new ResourceNotFoundException("评测运行不存在: " + evalRunId));
        if (evalRun.recallAtK() == null) {
            throw new IllegalStateException("评测运行尚未完成，无法设为基线");
        }
        LibraryProfile profile = repository.findLatestLibraryProfile(libraryId)
                .orElseThrow(() -> new IllegalStateException("知识库 Profile 不存在: " + libraryId));
        KnowledgeLibrary library = repository.findLibrary(libraryId).orElseThrow();
        Instant now = Instant.now();
        RetrievalEvalBaseline baseline = new RetrievalEvalBaseline(
                libraryId,
                evalRunId,
                profile.profileId(),
                library.activeIndexGenerationId(),
                evalRun.recallAtK(),
                evalRun.hitK(),
                now,
                now
        );
        return ResultMapper.toRetrievalEvalBaselineResult(repository.saveRetrievalEvalBaseline(baseline));
    }

    public List<RetrievalEvalSampleResult> importSamples(UUID libraryId, ImportRetrievalEvalSamplesCommand command) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.WRITE);
        requireLibrary(libraryId);
        if (Boolean.TRUE.equals(command.replaceExisting())) {
            repository.deleteRetrievalEvalSamplesByLibrary(libraryId);
        }
        List<RetrievalEvalSampleResult> imported = new ArrayList<>();
        for (CreateRetrievalEvalSampleCommand sample : command.samples()) {
            imported.add(createSample(libraryId, sample));
        }
        return List.copyOf(imported);
    }

    public List<RetrievalEvalSampleResult> bootstrapSampleDocuments(UUID libraryId, boolean replaceExisting) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.WRITE);
        ImportRetrievalEvalSamplesCommand command = SampleDocumentsEvalCatalog.buildImportCommand(replaceExisting);
        return importSamples(libraryId, command);
    }

    public List<RetrievalEvalSampleResult> generateDrafts(UUID libraryId, GenerateRetrievalEvalDraftsCommand command) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.WRITE);
        requireLibrary(libraryId);
        GenerateRetrievalEvalDraftsCommand effective = command == null
                ? new GenerateRetrievalEvalDraftsCommand(null, List.of(), false)
                : command;
        return evalDraftService.generateForCommand(libraryId, effective);
    }

    private void maybeRefreshBaseline(
            UUID libraryId,
            LibraryProfile profile,
            RetrievalEvalRunResult result,
            RetrievalEvalBaseline existing
    ) {
        if (result.recallAtK() == null) {
            return;
        }
        KnowledgeLibrary library = repository.findLibrary(libraryId).orElseThrow();
        Instant now = Instant.now();
        if (existing == null || result.recallAtK() >= existing.recallAtK()) {
            repository.saveRetrievalEvalBaseline(new RetrievalEvalBaseline(
                    libraryId,
                    result.evalRunId(),
                    profile.profileId(),
                    library.activeIndexGenerationId(),
                    result.recallAtK(),
                    result.hitK(),
                    existing == null ? now : existing.createdAt(),
                    now
            ));
        }
    }

    private RetrievalEvalRunResult completeRun(RetrievalEvalRun running, List<RetrievalEvalResultItem> items, String message) {
        RetrievalEvalRun completed = new RetrievalEvalRun(
                running.evalRunId(),
                running.libraryId(),
                RetrievalEvalRunStatus.SUCCEEDED,
                running.hitK(),
                0,
                0,
                null,
                running.retrievalPolicy(),
                message,
                running.createdAt(),
                Instant.now()
        );
        repository.saveRetrievalEvalRun(completed);
        return ResultMapper.toRetrievalEvalRunResult(completed, items);
    }

    private static int resolveHitK(Integer commandHitK, List<RetrievalEvalSample> samples) {
        if (commandHitK != null) {
            return Math.max(1, commandHitK);
        }
        return samples.stream().mapToInt(RetrievalEvalSample::hitRank).max().orElse(DEFAULT_HIT_RANK);
    }

    private static void validateCriteria(
            List<UUID> expectedDocumentIds,
            List<String> expectedSourceUris,
            List<String> groundTruthContexts
    ) {
        boolean hasDocuments = expectedDocumentIds != null && !expectedDocumentIds.isEmpty();
        boolean hasSources = expectedSourceUris != null && expectedSourceUris.stream().anyMatch(s -> s != null && !s.isBlank());
        boolean hasGroundTruth = groundTruthContexts != null && groundTruthContexts.stream().anyMatch(s -> s != null && !s.isBlank());
        if (!hasDocuments && !hasSources && !hasGroundTruth) {
            throw new IllegalArgumentException("至少配置 expectedDocumentIds、expectedSourceUris 或 groundTruthContexts 之一");
        }
    }

    private void requireLibrary(UUID libraryId) {
        repository.findLibrary(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("知识库不存在: " + libraryId));
    }

    private RetrievalEvalSample requireSample(UUID libraryId, UUID sampleId) {
        RetrievalEvalSample sample = repository.findRetrievalEvalSample(sampleId)
                .orElseThrow(() -> new ResourceNotFoundException("黄金样本不存在: " + sampleId));
        if (!sample.libraryId().equals(libraryId)) {
            throw new ResourceNotFoundException("黄金样本不存在: " + sampleId);
        }
        return sample;
    }

    static RetrievalHitCheckResult toHitCheckResult(RetrievalHitEvaluator.HitEvaluation evaluation) {
        return new RetrievalHitCheckResult(
                evaluation.hit(),
                evaluation.hitRankUsed(),
                evaluation.firstHitRank(),
                evaluation.matchedDocumentId(),
                evaluation.matchedChunkId(),
                evaluation.matchType(),
                evaluation.retrievedCount(),
                evaluation.failureReason()
        );
    }

    static double readRecallThreshold(LibraryProfile profile) {
        if (profile.options() == null) {
            return 0.85;
        }
        Object raw = profile.options().get("promoteRecallAtK");
        if (raw instanceof Number number) {
            return Math.min(1.0, Math.max(0.0, number.doubleValue()));
        }
        if (raw != null) {
            try {
                return Math.min(1.0, Math.max(0.0, Double.parseDouble(String.valueOf(raw))));
            } catch (NumberFormatException ignored) {
                return 0.85;
            }
        }
        return 0.85;
    }

    static double readRegressionDeltaMax(LibraryProfile profile) {
        if (profile.options() == null) {
            return 0.02;
        }
        Object raw = profile.options().get("promoteRecallRegressionDeltaMax");
        if (raw instanceof Number number) {
            return Math.min(1.0, Math.max(0.0, number.doubleValue()));
        }
        if (raw != null) {
            try {
                return Math.min(1.0, Math.max(0.0, Double.parseDouble(String.valueOf(raw))));
            } catch (NumberFormatException ignored) {
                return 0.02;
            }
        }
        return 0.02;
    }

    private static String formatPercent(double value) {
        return String.format("%.1f%%", value * 100);
    }

    private static String abbreviate(String text) {
        if (text == null || text.length() <= 40) {
            return text;
        }
        return text.substring(0, 37) + "...";
    }

    public record PromoteEvalSummary(
            boolean configured,
            boolean passed,
            double recallThreshold,
            double regressionDeltaMax,
            double currentRecallAtK,
            double baselineRecallAtK,
            double regressionDelta,
            UUID baselineEvalRunId,
            RetrievalEvalRunResult latestRun,
            List<String> failures,
            List<String> messages
    ) {
        static PromoteEvalSummary notConfigured(String message) {
            return new PromoteEvalSummary(
                    false,
                    false,
                    0.85,
                    0.02,
                    0.0,
                    Double.NaN,
                    0.0,
                    null,
                    null,
                    List.of(message),
                    List.of(message)
            );
        }
    }
}
