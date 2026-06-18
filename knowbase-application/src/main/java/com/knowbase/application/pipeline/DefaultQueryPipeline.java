package com.knowbase.application.pipeline;

import com.knowbase.agent.LibraryRouter;
import com.knowbase.agent.RouteRequest;
import com.knowbase.application.security.AccessControlService;
import com.knowbase.application.security.AccessDeniedException;
import com.knowbase.domain.model.AgentVersion;
import com.knowbase.domain.model.EvidencePack;
import com.knowbase.domain.model.QueryRun;
import com.knowbase.domain.model.TokenizerProfile;
import com.knowbase.domain.observability.PipelineObserver;
import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.domain.security.AclPermission;
import com.knowbase.domain.status.QueryRunStatus;
import com.knowbase.model.ChatCompletion;
import com.knowbase.model.ChatModelClient;
import com.knowbase.model.ChatRequest;
import com.knowbase.retrieval.ContextPacker;
import com.knowbase.retrieval.EvidenceBuilder;
import com.knowbase.retrieval.PackedContext;
import com.knowbase.retrieval.RetrievalRequest;
import com.knowbase.retrieval.Retriever;
import com.knowbase.tokenizer.ModelTokenizer;
import com.knowbase.tokenizer.ProfileBackedTokenizer;
import com.knowbase.tokenizer.TokenizerRegistry;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class DefaultQueryPipeline {

    private static final int DEFAULT_MAX_CONTEXT_TOKENS = 4096;

    private final KnowbaseRepository repository;
    private final LibraryRouter libraryRouter;
    private final Retriever retriever;
    private final EvidenceBuilder evidenceBuilder;
    private final ContextPacker contextPacker;
    private final ChatModelClient chatModelClient;
    private final TokenizerRegistry tokenizerRegistry;
    private final PipelineObserver pipelineObserver;
    private final AccessControlService accessControlService;

    public DefaultQueryPipeline(
            KnowbaseRepository repository,
            LibraryRouter libraryRouter,
            Retriever retriever,
            EvidenceBuilder evidenceBuilder,
            ContextPacker contextPacker,
            ChatModelClient chatModelClient,
            TokenizerRegistry tokenizerRegistry,
            PipelineObserver pipelineObserver
    ) {
        this(
                repository,
                libraryRouter,
                retriever,
                evidenceBuilder,
                contextPacker,
                chatModelClient,
                tokenizerRegistry,
                pipelineObserver,
                null
        );
    }

    public DefaultQueryPipeline(
            KnowbaseRepository repository,
            LibraryRouter libraryRouter,
            Retriever retriever,
            EvidenceBuilder evidenceBuilder,
            ContextPacker contextPacker,
            ChatModelClient chatModelClient,
            TokenizerRegistry tokenizerRegistry,
            PipelineObserver pipelineObserver,
            AccessControlService accessControlService
    ) {
        this.repository = repository;
        this.libraryRouter = libraryRouter;
        this.retriever = retriever;
        this.evidenceBuilder = evidenceBuilder;
        this.contextPacker = contextPacker;
        this.chatModelClient = chatModelClient;
        this.tokenizerRegistry = tokenizerRegistry;
        this.pipelineObserver = pipelineObserver == null ? new com.knowbase.domain.observability.NoopPipelineObserver() : pipelineObserver;
        this.accessControlService = accessControlService;
    }

    public QueryRun run(UUID queryRunId, UUID agentId, UUID agentVersionId, String question, List<UUID> debugLibraryIds) {
        AgentVersion agentVersion = resolveAgentVersion(agentId, agentVersionId);
        if (!agentVersion.published()) {
            throw new IllegalStateException("正式问答仅允许使用已发布的智能体版本");
        }

        QueryRun created = saveStatus(queryRunId, agentId, agentVersion.agentVersionId(), question, QueryRunStatus.CREATED, null, null, 0, 0);
        String traceId = created.traceId();
        UUID querySpan = null;
        UUID routingSpan = null;
        UUID retrievalSpan = null;
        UUID generationSpan = null;
        try {
            querySpan = pipelineObserver.startSpan("query", queryRunId, "pipeline", Map.of(
                    "agentId", agentId.toString(),
                    "traceId", traceId
            ));
            List<UUID> libraryIds = debugLibraryIds == null || debugLibraryIds.isEmpty()
                    ? libraryRouter.route(new RouteRequest(
                            agentVersion.agentVersionId(),
                            question,
                            agentVersion.libraryIds(),
                            agentVersion.routingPolicy()
                    ))
                    : validateDebugLibraryScope(agentVersion, debugLibraryIds);
            saveStatus(queryRunId, agentId, agentVersion.agentVersionId(), question, QueryRunStatus.ROUTING, null, null, 0, 0);
            routingSpan = pipelineObserver.startSpan("query", queryRunId, "routing", Map.of("traceId", traceId));

            List<com.knowbase.retrieval.RetrievalCandidate> candidates = retriever.retrieve(new RetrievalRequest(
                    queryRunId,
                    question,
                    libraryIds,
                    agentVersion.retrievalPolicy()
            ));
            pipelineObserver.finishSpan(routingSpan, "SUCCEEDED", Map.of("libraryCount", libraryIds.size(), "traceId", traceId));
            routingSpan = null;
            saveStatus(queryRunId, agentId, agentVersion.agentVersionId(), question, QueryRunStatus.RETRIEVING, null, null, 0, 0);
            retrievalSpan = pipelineObserver.startSpan("query", queryRunId, "retrieval", Map.of("traceId", traceId));

            EvidencePack evidencePack = limitEvidence(evidenceBuilder.build(candidates), agentVersion.retrievalPolicy());
            pipelineObserver.finishSpan(retrievalSpan, "SUCCEEDED", Map.of(
                    "candidateCount", candidates.size(),
                    "evidenceCount", evidencePack.segments().size(),
                    "traceId", traceId
            ));
            retrievalSpan = null;
            ModelTokenizer chatTokenizer = resolveChatTokenizer(agentVersion);
            PackedContext packedContext = contextPacker.pack(
                    evidencePack,
                    chatTokenizer,
                    readInt(agentVersion.answerPolicy(), "maxContextTokens", DEFAULT_MAX_CONTEXT_TOKENS)
            );
            saveStatus(queryRunId, agentId, agentVersion.agentVersionId(), question, QueryRunStatus.GENERATING, null, evidencePack, 0, 0);
            generationSpan = pipelineObserver.startSpan("query", queryRunId, "generation", Map.of("traceId", traceId));

            boolean refuse = shouldRefuse(agentVersion.answerPolicy(), evidencePack);
            ChatCompletion completion;
            if (refuse) {
                completion = new ChatCompletion("未找到足够证据，无法基于知识库回答该问题。", 0, 0, "");
            } else {
                completion = chatModelClient.complete(new ChatRequest(
                        agentVersion.systemPrompt(),
                        question,
                        packedContext.context(),
                        Map.of()
                ));
            }

            EvidencePack finalEvidence = new EvidencePack(
                    evidencePack.evidencePackId(),
                    evidencePack.segments(),
                    packedContext.citations(),
                    packedContext.tokenCount(),
                    chatTokenizer.tokenizerId(),
                    chatTokenizer.tokenizerVersion()
            );

            Instant now = Instant.now();
            QueryRun succeeded = new QueryRun(
                    queryRunId,
                    agentId,
                    agentVersion.agentVersionId(),
                    QueryRunStatus.SUCCEEDED,
                    question,
                    completion.answer(),
                    finalEvidence,
                    traceId,
                    completion.promptTokens() > 0 ? completion.promptTokens() : packedContext.tokenCount(),
                    completion.completionTokens(),
                    created.createdAt(),
                    now
            );
            pipelineObserver.finishSpan(generationSpan, "SUCCEEDED", Map.of("traceId", traceId));
            generationSpan = null;
            pipelineObserver.finishSpan(querySpan, "SUCCEEDED", Map.of("traceId", traceId));
            querySpan = null;
            return repository.saveQueryRun(succeeded);
        } catch (RuntimeException exception) {
            Map<String, Object> errorAttributes = Map.of(
                    "traceId", traceId,
                    "error", exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage()
            );
            finishSpanIfOpen(generationSpan, errorAttributes);
            finishSpanIfOpen(retrievalSpan, errorAttributes);
            finishSpanIfOpen(routingSpan, errorAttributes);
            finishSpanIfOpen(querySpan, errorAttributes);
            saveStatus(queryRunId, agentId, agentVersion.agentVersionId(), question, QueryRunStatus.FAILED, null, null, 0, 0);
            throw exception;
        }
    }

    private void finishSpanIfOpen(UUID spanId, Map<String, Object> attributes) {
        if (spanId != null) {
            pipelineObserver.finishSpan(spanId, "FAILED", attributes);
        }
    }

    private AgentVersion resolveAgentVersion(UUID agentId, UUID agentVersionId) {
        if (agentVersionId != null) {
            AgentVersion version = repository.findAgentVersion(agentVersionId)
                    .orElseThrow(() -> new IllegalArgumentException("智能体版本不存在: " + agentVersionId));
            if (!version.agentId().equals(agentId)) {
                throw new IllegalArgumentException("智能体版本不属于当前智能体: " + agentVersionId);
            }
            return version;
        }
        return repository.findPublishedAgentVersion(agentId)
                .orElseThrow(() -> new IllegalArgumentException("未找到已发布的智能体版本: " + agentId));
    }

    private List<UUID> validateDebugLibraryScope(AgentVersion agentVersion, List<UUID> debugLibraryIds) {
        Set<UUID> allowed = new HashSet<>(agentVersion.libraryIds());
        List<UUID> invalid = debugLibraryIds.stream()
                .filter(libraryId -> !allowed.contains(libraryId))
                .toList();
        if (!invalid.isEmpty()) {
            throw new IllegalArgumentException("调试知识库不在智能体绑定范围内: " + invalid);
        }
        if (accessControlService != null) {
            List<UUID> denied = debugLibraryIds.stream()
                    .filter(libraryId -> !accessControlService.canAccessLibrary(libraryId, AclPermission.READ))
                    .toList();
            if (!denied.isEmpty()) {
                throw new AccessDeniedException("无权访问调试知识库: " + denied);
            }
        }
        return List.copyOf(debugLibraryIds);
    }

    private ModelTokenizer resolveChatTokenizer(AgentVersion agentVersion) {
        ModelTokenizer delegate = tokenizerRegistry.getTokenizer(
                chatModelClient.provider(),
                chatModelClient.modelName()
        );
        if (agentVersion.chatTokenizerProfileId() == null) {
            return delegate;
        }
        TokenizerProfile profile = repository.findTokenizerProfile(agentVersion.chatTokenizerProfileId())
                .orElseThrow(() -> new IllegalStateException(
                        "Chat Tokenizer Profile 不存在: " + agentVersion.chatTokenizerProfileId()
                ));
        return new ProfileBackedTokenizer(
                profile.tokenizerId(),
                profile.tokenizerVersion(),
                profile.approximate(),
                delegate
        );
    }

    private QueryRun saveStatus(
            UUID queryRunId,
            UUID agentId,
            UUID agentVersionId,
            String question,
            QueryRunStatus status,
            String answer,
            EvidencePack evidencePack,
            int promptTokens,
            int completionTokens
    ) {
        Instant now = Instant.now();
        QueryRun existing = repository.findQueryRun(queryRunId).orElse(null);
        Instant completedAt = switch (status) {
            case SUCCEEDED, FAILED -> now;
            default -> existing == null ? null : existing.completedAt();
        };
        QueryRun updated = new QueryRun(
                queryRunId,
                agentId,
                agentVersionId,
                status,
                question,
                answer,
                evidencePack,
                existing == null ? UUID.randomUUID().toString() : existing.traceId(),
                promptTokens,
                completionTokens,
                existing == null ? now : existing.createdAt(),
                completedAt
        );
        return repository.saveQueryRun(updated);
    }

    private static boolean shouldRefuse(Map<String, Object> answerPolicy, EvidencePack evidencePack) {
        int minEvidenceCount = readInt(answerPolicy, "minEvidenceCount", 1);
        boolean evidenceLow = evidencePack.segments().size() < minEvidenceCount;
        if (answerPolicy == null || !Boolean.TRUE.equals(answerPolicy.get("refuseWhenEvidenceLow"))) {
            return evidencePack.segments().isEmpty();
        }
        return evidenceLow;
    }

    private static EvidencePack limitEvidence(EvidencePack evidencePack, Map<String, Object> retrievalPolicy) {
        int maxEvidence = readInt(retrievalPolicy, "maxEvidence", 12);
        List<com.knowbase.domain.model.EvidenceSegment> segments = evidencePack.segments().stream()
                .limit(Math.max(1, maxEvidence))
                .toList();
        java.util.Set<UUID> chunkIds = segments.stream()
                .map(com.knowbase.domain.model.EvidenceSegment::chunkId)
                .collect(java.util.stream.Collectors.toSet());
        List<com.knowbase.domain.model.Citation> citations = evidencePack.citations().stream()
                .filter(citation -> chunkIds.contains(citation.chunkId()))
                .toList();
        return new EvidencePack(
                evidencePack.evidencePackId(),
                segments,
                citations,
                evidencePack.contextTokens(),
                evidencePack.tokenizerId(),
                evidencePack.tokenizerVersion()
        );
    }

    private static int readInt(Map<String, Object> policy, String key, int defaultValue) {
        if (policy == null || policy.get(key) == null) {
            return defaultValue;
        }
        Object value = policy.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }
}
