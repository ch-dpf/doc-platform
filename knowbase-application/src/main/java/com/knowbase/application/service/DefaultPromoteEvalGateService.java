package com.knowbase.application.service;

import com.knowbase.api.result.PromoteEvalGateResult;
import com.knowbase.domain.repository.KnowbaseRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class DefaultPromoteEvalGateService {

    private final KnowbaseRepository repository;
    private final DefaultRetrievalEvalService retrievalEvalService;
    private final boolean enabled;

    public DefaultPromoteEvalGateService(
            KnowbaseRepository repository,
            DefaultRetrievalEvalService retrievalEvalService,
            boolean enabled
    ) {
        this.repository = repository;
        this.retrievalEvalService = retrievalEvalService;
        this.enabled = enabled;
    }

    public PromoteEvalGateResult evaluate(UUID libraryId) {
        if (!enabled) {
            return PromoteEvalGateResult.skipped(libraryId);
        }
        repository.findLibrary(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("知识库不存在: " + libraryId));
        DefaultRetrievalEvalService.PromoteEvalSummary summary = retrievalEvalService.summarizeForPromoteGate(libraryId);
        List<String> messages = new ArrayList<>(summary.messages());
        List<String> failures = new ArrayList<>(summary.failures());
        if (!summary.configured()) {
            failures.add("未配置黄金样本，promote 评测门禁拒绝通过");
        }
        boolean passed = summary.configured() && summary.passed();
        UUID latestEvalRunId = summary.latestRun() == null ? null : summary.latestRun().evalRunId();
        return new PromoteEvalGateResult(
                libraryId,
                true,
                passed,
                List.copyOf(failures),
                List.copyOf(messages),
                summary.currentRecallAtK(),
                Double.isNaN(summary.baselineRecallAtK()) ? null : summary.baselineRecallAtK(),
                summary.recallThreshold(),
                summary.regressionDelta(),
                summary.regressionDeltaMax(),
                latestEvalRunId,
                summary.baselineEvalRunId()
        );
    }
}
