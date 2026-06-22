package com.knowbase.application.service;

import com.knowbase.api.command.CreateEvalRunCommand;
import com.knowbase.api.result.EvalRunResult;
import com.knowbase.api.result.EvalSampleResult;
import com.knowbase.application.pipeline.DefaultQueryPipeline;
import com.knowbase.domain.model.AgentVersion;
import com.knowbase.domain.model.EvalRun;
import com.knowbase.domain.model.EvalSample;
import com.knowbase.domain.model.QueryRun;
import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.domain.repository.ObservabilityRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DefaultEvalService {

    private final ObservabilityRepository observabilityRepository;
    private final KnowbaseRepository knowbaseRepository;
    private final DefaultQueryPipeline queryPipeline;

    public DefaultEvalService(
            ObservabilityRepository observabilityRepository,
            KnowbaseRepository knowbaseRepository,
            DefaultQueryPipeline queryPipeline
    ) {
        this.observabilityRepository = observabilityRepository;
        this.knowbaseRepository = knowbaseRepository;
        this.queryPipeline = queryPipeline;
    }

    public EvalRunResult create(CreateEvalRunCommand command) {
        UUID evalRunId = UUID.randomUUID();
        Instant now = Instant.now();
        AgentVersion agentVersion = command.agentId() == null
                ? null
                : knowbaseRepository.findPublishedAgentVersion(command.agentId()).orElse(null);
        EvalRun running = new EvalRun(
                evalRunId,
                command.tenantId(),
                command.agentId(),
                command.evalType(),
                "RUNNING",
                Map.of(),
                now,
                null
        );
        observabilityRepository.saveEvalRun(running);

        List<EvalSampleResult> sampleResults = new ArrayList<>();
        double totalScore = 0;
        int scored = 0;
        for (CreateEvalRunCommand.EvalSampleInput input : command.samples()) {
            EvalSampleResult sampleResult = evaluateSample(evalRunId, command.agentId(), agentVersion, input);
            sampleResults.add(sampleResult);
            if (sampleResult.score() != null) {
                totalScore += sampleResult.score();
                scored++;
            }
        }
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("sampleCount", sampleResults.size());
        metrics.put("averageScore", scored == 0 ? 0.0 : totalScore / scored);
        metrics.put("citation_accuracy", averageCitationAccuracy(sampleResults));
        metrics.put("faithfulness", averageFaithfulness(sampleResults));
        EvalRun finished = new EvalRun(
                evalRunId,
                command.tenantId(),
                command.agentId(),
                command.evalType(),
                "COMPLETED",
                Map.copyOf(metrics),
                now,
                Instant.now()
        );
        observabilityRepository.saveEvalRun(finished);
        return new EvalRunResult(
                finished.evalRunId(),
                finished.tenantId(),
                finished.agentId(),
                finished.evalType(),
                finished.status(),
                finished.metrics(),
                sampleResults,
                finished.createdAt(),
                finished.finishedAt()
        );
    }

    public EvalRunResult get(UUID evalRunId) {
        EvalRun evalRun = observabilityRepository.findEvalRun(evalRunId)
                .orElseThrow(() -> new ResourceNotFoundException("评测运行不存在: " + evalRunId));
        List<EvalSampleResult> samples = observabilityRepository.listEvalSamples(evalRunId).stream()
                .map(DefaultEvalService::toSampleResult)
                .toList();
        return new EvalRunResult(
                evalRun.evalRunId(),
                evalRun.tenantId(),
                evalRun.agentId(),
                evalRun.evalType(),
                evalRun.status(),
                evalRun.metrics(),
                samples,
                evalRun.createdAt(),
                evalRun.finishedAt()
        );
    }

    public List<EvalRunResult> list(String tenantId, UUID agentId) {
        return observabilityRepository.listEvalRuns(tenantId, agentId).stream()
                .map(run -> get(run.evalRunId()))
                .toList();
    }

    private EvalSampleResult evaluateSample(
            UUID evalRunId,
            UUID agentId,
            AgentVersion agentVersion,
            CreateEvalRunCommand.EvalSampleInput input
    ) {
        UUID sampleId = UUID.randomUUID();
        Instant now = Instant.now();
        String actualAnswer = null;
        Double score = null;
        Map<String, Object> metrics = new HashMap<>();
        if (agentId != null && agentVersion != null) {
            QueryRun queryRun = queryPipeline.run(UUID.randomUUID(), agentId, agentVersion.agentVersionId(), input.question(), List.of());
            actualAnswer = queryRun.answer();
            score = scoreAnswer(input.expectedAnswer(), actualAnswer);
            metrics.put("queryRunId", queryRun.queryRunId().toString());
            metrics.put("traceId", queryRun.traceId() == null ? null : queryRun.traceId());
            metrics.put("evidenceCount", queryRun.evidencePack() == null ? 0 : queryRun.evidencePack().segments().size());
            metrics.put("citationCount", queryRun.evidencePack() == null ? 0 : queryRun.evidencePack().citations().size());
        }
        EvalSample sample = new EvalSample(
                sampleId,
                evalRunId,
                input.question(),
                input.expectedAnswer(),
                actualAnswer,
                score,
                Map.copyOf(metrics),
                now
        );
        observabilityRepository.saveEvalSample(sample);
        return toSampleResult(sample);
    }

    private static double averageCitationAccuracy(List<EvalSampleResult> samples) {
        long cited = samples.stream()
                .filter(sample -> sample.metrics() != null && sample.metrics().get("evidenceCount") instanceof Number number && number.intValue() > 0)
                .count();
        return samples.isEmpty() ? 0.0 : (double) cited / samples.size();
    }

    private static double averageFaithfulness(List<EvalSampleResult> samples) {
        double total = 0.0;
        int counted = 0;
        for (EvalSampleResult sample : samples) {
            if (sample.actualAnswer() == null || sample.actualAnswer().isBlank()) {
                continue;
            }
            Object evidenceCount = sample.metrics() == null ? null : sample.metrics().get("evidenceCount");
            if (evidenceCount instanceof Number number && number.intValue() > 0) {
                total += 1.0;
                counted++;
            }
        }
        return counted == 0 ? 0.0 : total / counted;
    }

    private static Double scoreAnswer(String expected, String actual) {
        if (expected == null || expected.isBlank() || actual == null || actual.isBlank()) {
            return null;
        }
        String normalizedExpected = expected.trim().toLowerCase();
        String normalizedActual = actual.trim().toLowerCase();
        if (normalizedActual.contains(normalizedExpected)) {
            return 1.0;
        }
        String[] tokens = normalizedExpected.split("\\s+");
        long hits = java.util.Arrays.stream(tokens).filter(normalizedActual::contains).count();
        return tokens.length == 0 ? 0.0 : (double) hits / tokens.length;
    }

    private static EvalSampleResult toSampleResult(EvalSample sample) {
        return new EvalSampleResult(
                sample.sampleId(),
                sample.evalRunId(),
                sample.question(),
                sample.expectedAnswer(),
                sample.actualAnswer(),
                sample.score(),
                sample.metrics(),
                sample.createdAt()
        );
    }
}
