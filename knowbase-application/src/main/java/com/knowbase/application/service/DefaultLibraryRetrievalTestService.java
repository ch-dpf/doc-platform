package com.knowbase.application.service;

import com.knowbase.agent.QuestionAnalysis;
import com.knowbase.agent.QuestionAnalyzer;
import com.knowbase.api.command.CreateLibraryRetrievalTestCommand;
import com.knowbase.api.result.LibraryRetrievalTestResult;
import com.knowbase.application.mapper.ResultMapper;
import com.knowbase.application.security.AccessControlService;
import com.knowbase.domain.model.AgentVersion;
import com.knowbase.domain.model.EvidencePack;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.domain.model.TokenizerProfile;
import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.domain.security.AclPermission;
import com.knowbase.domain.status.AgentVersionStatus;
import com.knowbase.model.ChatModelClient;
import com.knowbase.retrieval.ContextPacker;
import com.knowbase.retrieval.EvidenceBuilder;
import com.knowbase.retrieval.PackedContext;
import com.knowbase.retrieval.RetrievalCandidate;
import com.knowbase.retrieval.RetrievalPlan;
import com.knowbase.retrieval.RetrievalPlanner;
import com.knowbase.retrieval.RetrievalPostProcessor;
import com.knowbase.retrieval.RetrievalRequest;
import com.knowbase.retrieval.Retriever;
import com.knowbase.tokenizer.ModelTokenizer;
import com.knowbase.tokenizer.ProfileBackedTokenizer;
import com.knowbase.tokenizer.TokenizerRegistry;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DefaultLibraryRetrievalTestService {

    private static final int DEFAULT_MAX_CONTEXT_TOKENS = 4096;
    private static final UUID SYNTHETIC_AGENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SYNTHETIC_VERSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private final KnowbaseRepository repository;
    private final AccessControlService accessControlService;
    private final QuestionAnalyzer questionAnalyzer;
    private final RetrievalPlanner retrievalPlanner;
    private final Retriever retriever;
    private final RetrievalPostProcessor retrievalPostProcessor;
    private final EvidenceBuilder evidenceBuilder;
    private final ContextPacker contextPacker;
    private final ChatModelClient chatModelClient;
    private final TokenizerRegistry tokenizerRegistry;

    public DefaultLibraryRetrievalTestService(
            KnowbaseRepository repository,
            AccessControlService accessControlService,
            QuestionAnalyzer questionAnalyzer,
            RetrievalPlanner retrievalPlanner,
            Retriever retriever,
            RetrievalPostProcessor retrievalPostProcessor,
            EvidenceBuilder evidenceBuilder,
            ContextPacker contextPacker,
            ChatModelClient chatModelClient,
            TokenizerRegistry tokenizerRegistry
    ) {
        this.repository = repository;
        this.accessControlService = accessControlService;
        this.questionAnalyzer = questionAnalyzer;
        this.retrievalPlanner = retrievalPlanner;
        this.retriever = retriever;
        this.retrievalPostProcessor = retrievalPostProcessor;
        this.evidenceBuilder = evidenceBuilder;
        this.contextPacker = contextPacker;
        this.chatModelClient = chatModelClient;
        this.tokenizerRegistry = tokenizerRegistry;
    }

    public LibraryRetrievalTestResult run(UUID libraryId, CreateLibraryRetrievalTestCommand command) {
        return execute(libraryId, command).result();
    }

    public RetrievalExecution executeRetrieval(UUID libraryId, CreateLibraryRetrievalTestCommand command) {
        return execute(libraryId, command);
    }

    public List<RetrievalCandidate> retrieveRankedCandidates(UUID libraryId, CreateLibraryRetrievalTestCommand command) {
        return execute(libraryId, command).rankedCandidates();
    }

    private RetrievalExecution execute(UUID libraryId, CreateLibraryRetrievalTestCommand command) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.READ);
        repository.findLibrary(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("知识库不存在: " + libraryId));
        LibraryProfile profile = repository.findLatestLibraryProfile(libraryId)
                .orElseThrow(() -> new IllegalStateException("知识库 Profile 不存在: " + libraryId));

        UUID testId = UUID.randomUUID();
        Map<String, Object> retrievalPolicy = buildRetrievalPolicy(profile, command.retrievalPolicyOverride());
        Map<String, Object> answerPolicy = mergePolicy(defaultAnswerPolicy(), command.answerPolicyOverride());

        QuestionAnalysis analysis = questionAnalyzer.analyze(command.question(), Map.of());
        AgentVersion syntheticVersion = new AgentVersion(
                SYNTHETIC_VERSION_ID,
                SYNTHETIC_AGENT_ID,
                1,
                AgentVersionStatus.PUBLISHED,
                "library_hit_test",
                List.of(libraryId),
                Map.of(),
                retrievalPolicy,
                answerPolicy,
                "",
                profile.embeddingTokenizerProfileId(),
                true,
                Instant.now()
        );
        RetrievalPlan retrievalPlan = retrievalPlanner.plan(syntheticVersion, analysis, List.of(libraryId));

        List<RetrievalCandidate> rawCandidates = retriever.retrieve(new RetrievalRequest(
                testId,
                analysis.normalizedQuestion(),
                retrievalPlan.libraryIds(),
                retrievalPlan.retrievalPolicy()
        ));
        List<RetrievalCandidate> fusedCandidates = retrievalPostProcessor.fuse(rawCandidates, retrievalPlan.retrievalPolicy());
        List<RetrievalCandidate> candidates = retrievalPostProcessor.rerank(fusedCandidates, retrievalPlan.retrievalPolicy());
        EvidencePack evidencePack = evidenceBuilder.build(candidates);
        ModelTokenizer chatTokenizer = resolveChatTokenizer(profile);
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
        int explainLimit = readInt(retrievalPolicy, "maxCandidates", 24);
        Map<String, Object> trace = new HashMap<>();
        trace.put("retrievalPolicy", retrievalPolicy);
        trace.put("answerPolicy", answerPolicy);
        trace.put("retrievalMode", stringValue(retrievalPolicy, "retrievalMode", "hybrid"));
        trace.put("fusion", stringValue(retrievalPolicy, "fusion", "score"));
        trace.put("rerank", stringValue(retrievalPolicy, "rerank", "none"));
        trace.put("maxCandidates", explainLimit);
        trace.put("maxEvidence", readInt(retrievalPolicy, "maxEvidence", 12));
        trace.put("topKPerLibrary", readInt(retrievalPolicy, "topKPerLibrary", profile.retrievalTopK()));
        trace.put("rawCandidateCount", rawCandidates.size());
        trace.put("fusedCandidateCount", fusedCandidates.size());
        trace.put("evidenceCount", finalEvidencePack.segments().size());
        trace.put("explain", RetrievalExplainBuilder.buildRankedExplain(candidates, explainLimit));
        LibraryRetrievalTestResult result = new LibraryRetrievalTestResult(
                testId,
                libraryId,
                command.question(),
                candidates.size(),
                finalEvidencePack.segments().stream().map(ResultMapper::toEvidenceResult).toList(),
                finalEvidencePack.citations().stream().map(ResultMapper::toCitationResult).toList(),
                finalEvidencePack.contextTokens(),
                finalEvidencePack.tokenizerId(),
                finalEvidencePack.tokenizerVersion(),
                evidenceLow,
                Map.copyOf(trace),
                Instant.now()
        );
        return new RetrievalExecution(result, List.copyOf(candidates), retrievalPolicy);
    }

    public record RetrievalExecution(
            LibraryRetrievalTestResult result,
            List<RetrievalCandidate> rankedCandidates,
            Map<String, Object> retrievalPolicy
    ) {
    }

    public Map<String, Object> buildRetrievalPolicyForLibrary(UUID libraryId, Map<String, Object> override) {
        LibraryProfile profile = repository.findLatestLibraryProfile(libraryId)
                .orElseThrow(() -> new IllegalStateException("知识库 Profile 不存在: " + libraryId));
        return buildRetrievalPolicy(profile, override);
    }

    private Map<String, Object> buildRetrievalPolicy(LibraryProfile profile, Map<String, Object> override) {
        Map<String, Object> base = new HashMap<>();
        base.put("topKPerLibrary", profile.retrievalTopK());
        base.put("maxCandidates", 24);
        base.put("maxEvidence", 12);
        base.put("retrievalMode", "hybrid");
        base.put("fusion", "score");
        base.put("rerank", "none");
        if (profile.options() != null) {
            Object retrieval = profile.options().get("retrieval");
            if (retrieval instanceof Map<?, ?> retrievalMap) {
                retrievalMap.forEach((key, value) -> base.put(String.valueOf(key), value));
            }
        }
        return mergePolicy(base, override);
    }

    private static Map<String, Object> defaultAnswerPolicy() {
        return Map.of(
                "maxContextTokens", DEFAULT_MAX_CONTEXT_TOKENS,
                "minEvidenceCount", 1
        );
    }

    private ModelTokenizer resolveChatTokenizer(LibraryProfile profile) {
        ModelTokenizer delegate = tokenizerRegistry.getTokenizer(chatModelClient.provider(), chatModelClient.modelName());
        if (profile.embeddingTokenizerProfileId() == null) {
            return delegate;
        }
        TokenizerProfile tokenizerProfile = repository.findTokenizerProfile(profile.embeddingTokenizerProfileId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tokenizer Profile 不存在: " + profile.embeddingTokenizerProfileId()
                ));
        return new ProfileBackedTokenizer(
                tokenizerProfile.tokenizerId(),
                tokenizerProfile.tokenizerVersion(),
                tokenizerProfile.approximate(),
                delegate
        );
    }

    private static Map<String, Object> mergePolicy(Map<String, Object> base, Map<String, Object> override) {
        Map<String, Object> merged = new HashMap<>(base);
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
}
