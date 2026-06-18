package com.knowbase.application.service;

import com.knowbase.agent.LibraryRouter;
import com.knowbase.agent.RouteRequest;
import com.knowbase.api.command.CreateRetrievalTestCommand;
import com.knowbase.api.result.RetrievalTestResult;
import com.knowbase.application.mapper.ResultMapper;
import com.knowbase.application.usecase.RunRetrievalTestUseCase;
import com.knowbase.application.security.AccessControlService;
import com.knowbase.application.security.AccessDeniedException;
import com.knowbase.domain.model.AgentVersion;
import com.knowbase.domain.model.EvidencePack;
import com.knowbase.domain.model.TokenizerProfile;
import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.domain.security.AclPermission;
import com.knowbase.model.ChatModelClient;
import com.knowbase.retrieval.ContextPacker;
import com.knowbase.retrieval.EvidenceBuilder;
import com.knowbase.retrieval.PackedContext;
import com.knowbase.retrieval.RetrievalCandidate;
import com.knowbase.retrieval.RetrievalRequest;
import com.knowbase.retrieval.Retriever;
import com.knowbase.tokenizer.ModelTokenizer;
import com.knowbase.tokenizer.ProfileBackedTokenizer;
import com.knowbase.tokenizer.TokenizerRegistry;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DefaultRetrievalTestService implements RunRetrievalTestUseCase {

    private static final int DEFAULT_MAX_CONTEXT_TOKENS = 4096;

    private final KnowbaseRepository repository;
    private final LibraryRouter libraryRouter;
    private final Retriever retriever;
    private final EvidenceBuilder evidenceBuilder;
    private final ContextPacker contextPacker;
    private final ChatModelClient chatModelClient;
    private final TokenizerRegistry tokenizerRegistry;
    private final AccessControlService accessControlService;

    public DefaultRetrievalTestService(
            KnowbaseRepository repository,
            LibraryRouter libraryRouter,
            Retriever retriever,
            EvidenceBuilder evidenceBuilder,
            ContextPacker contextPacker,
            ChatModelClient chatModelClient,
            TokenizerRegistry tokenizerRegistry
    ) {
        this(repository, libraryRouter, retriever, evidenceBuilder, contextPacker, chatModelClient, tokenizerRegistry, null);
    }

    public DefaultRetrievalTestService(
            KnowbaseRepository repository,
            LibraryRouter libraryRouter,
            Retriever retriever,
            EvidenceBuilder evidenceBuilder,
            ContextPacker contextPacker,
            ChatModelClient chatModelClient,
            TokenizerRegistry tokenizerRegistry,
            AccessControlService accessControlService
    ) {
        this.repository = repository;
        this.libraryRouter = libraryRouter;
        this.retriever = retriever;
        this.evidenceBuilder = evidenceBuilder;
        this.contextPacker = contextPacker;
        this.chatModelClient = chatModelClient;
        this.tokenizerRegistry = tokenizerRegistry;
        this.accessControlService = accessControlService;
    }

    @Override
    public RetrievalTestResult run(UUID agentId, CreateRetrievalTestCommand command) {
        UUID testId = UUID.randomUUID();
        AgentVersion agentVersion = resolveAgentVersion(agentId, command.agentVersionId());
        Map<String, Object> retrievalPolicy = mergePolicy(agentVersion.retrievalPolicy(), command.retrievalPolicyOverride());
        Map<String, Object> answerPolicy = mergePolicy(agentVersion.answerPolicy(), command.answerPolicyOverride());
        List<UUID> routedLibraryIds = command.debugLibraryIds() == null || command.debugLibraryIds().isEmpty()
                ? libraryRouter.route(new RouteRequest(
                        agentVersion.agentVersionId(),
                        command.question(),
                        agentVersion.libraryIds(),
                        agentVersion.routingPolicy()
                ))
                : validateDebugLibraryScope(agentVersion, command.debugLibraryIds());

        List<RetrievalCandidate> candidates = retriever.retrieve(new RetrievalRequest(
                testId,
                command.question(),
                routedLibraryIds,
                retrievalPolicy
        ));
        EvidencePack evidencePack = evidenceBuilder.build(candidates);
        ModelTokenizer chatTokenizer = resolveChatTokenizer(agentVersion);
        PackedContext packedContext = contextPacker.pack(
                evidencePack,
                chatTokenizer,
                readInt(answerPolicy, "maxContextTokens", DEFAULT_MAX_CONTEXT_TOKENS)
        );
        EvidencePack finalEvidencePack = new EvidencePack(
                evidencePack.evidencePackId(),
                evidencePack.segments(),
                packedContext.citations(),
                packedContext.tokenCount(),
                chatTokenizer.tokenizerId(),
                chatTokenizer.tokenizerVersion()
        );
        boolean evidenceLow = finalEvidencePack.segments().size() < readInt(answerPolicy, "minEvidenceCount", 1);
        return new RetrievalTestResult(
                testId,
                agentId,
                agentVersion.agentVersionId(),
                command.question(),
                routedLibraryIds,
                candidates.size(),
                finalEvidencePack.segments().stream().map(ResultMapper::toEvidenceResult).toList(),
                finalEvidencePack.citations().stream().map(ResultMapper::toCitationResult).toList(),
                finalEvidencePack.contextTokens(),
                finalEvidencePack.tokenizerId(),
                finalEvidencePack.tokenizerVersion(),
                evidenceLow,
                Map.ofEntries(
                        Map.entry("retrievalPolicy", retrievalPolicy),
                        Map.entry("answerPolicy", answerPolicy),
                        Map.entry("fusion", stringValue(retrievalPolicy, "fusion", "score")),
                        Map.entry("rerank", stringValue(retrievalPolicy, "rerank", "none")),
                        Map.entry("balanceAcrossLibraries", booleanValue(retrievalPolicy, "balanceAcrossLibraries", false)),
                        Map.entry("maxCandidates", readInt(retrievalPolicy, "maxCandidates", 24)),
                        Map.entry("maxEvidence", readInt(retrievalPolicy, "maxEvidence", 12)),
                        Map.entry("maxContextTokens", readInt(answerPolicy, "maxContextTokens", DEFAULT_MAX_CONTEXT_TOKENS)),
                        Map.entry("candidateCount", candidates.size()),
                        Map.entry("evidenceCount", finalEvidencePack.segments().size()),
                        Map.entry("citationCount", finalEvidencePack.citations().size())
                ),
                Instant.now()
        );
    }

    private AgentVersion resolveAgentVersion(UUID agentId, UUID agentVersionId) {
        if (agentVersionId != null) {
            AgentVersion version = repository.findAgentVersion(agentVersionId)
                    .orElseThrow(() -> new ResourceNotFoundException("智能体版本不存在: " + agentVersionId));
            if (!version.agentId().equals(agentId)) {
                throw new IllegalArgumentException("智能体版本不属于当前智能体: " + agentVersionId);
            }
            return version;
        }
        return repository.findPublishedAgentVersion(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("未找到已发布的智能体版本: " + agentId));
    }

    private List<UUID> validateDebugLibraryScope(AgentVersion agentVersion, List<UUID> debugLibraryIds) {
        java.util.Set<UUID> allowed = new java.util.HashSet<>(agentVersion.libraryIds());
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
        ModelTokenizer delegate = tokenizerRegistry.getTokenizer(chatModelClient.provider(), chatModelClient.modelName());
        if (agentVersion.chatTokenizerProfileId() == null) {
            return delegate;
        }
        TokenizerProfile profile = repository.findTokenizerProfile(agentVersion.chatTokenizerProfileId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Chat Tokenizer Profile 不存在: " + agentVersion.chatTokenizerProfileId()
                ));
        return new ProfileBackedTokenizer(
                profile.tokenizerId(),
                profile.tokenizerVersion(),
                profile.approximate(),
                delegate
        );
    }

    private static Map<String, Object> mergePolicy(Map<String, Object> base, Map<String, Object> override) {
        java.util.HashMap<String, Object> merged = new java.util.HashMap<>();
        if (base != null) {
            merged.putAll(base);
        }
        if (override != null) {
            merged.putAll(override);
        }
        return Map.copyOf(merged);
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

    private static String stringValue(Map<String, Object> policy, String key, String defaultValue) {
        if (policy == null || policy.get(key) == null) {
            return defaultValue;
        }
        String value = String.valueOf(policy.get(key));
        return value.isBlank() ? defaultValue : value;
    }

    private static boolean booleanValue(Map<String, Object> policy, String key, boolean defaultValue) {
        if (policy == null || policy.get(key) == null) {
            return defaultValue;
        }
        Object value = policy.get(key);
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }
}
