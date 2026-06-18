package com.knowbase.application.service;

import com.knowbase.api.command.CreateKnowledgeAgentCommand;
import com.knowbase.api.facade.KnowbaseAgentFacade;
import com.knowbase.api.result.KnowledgeAgentResult;
import com.knowbase.application.mapper.ResultMapper;
import com.knowbase.application.usecase.CreateKnowledgeAgentUseCase;
import com.knowbase.domain.model.AgentVersion;
import com.knowbase.domain.model.KnowledgeAgent;
import com.knowbase.domain.model.SceneRulePreset;
import com.knowbase.application.security.AccessControlService;
import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.domain.security.AclPermission;
import com.knowbase.domain.status.AgentStatus;
import com.knowbase.domain.status.AgentVersionStatus;
import com.knowbase.preset.PresetCatalog;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DefaultKnowledgeAgentService implements CreateKnowledgeAgentUseCase, KnowbaseAgentFacade {

    private final KnowbaseRepository repository;
    private final PresetCatalog presetCatalog;
    private final AccessControlService accessControlService;

    public DefaultKnowledgeAgentService(
            KnowbaseRepository repository,
            PresetCatalog presetCatalog,
            AccessControlService accessControlService
    ) {
        this.repository = repository;
        this.presetCatalog = presetCatalog;
        this.accessControlService = accessControlService;
    }

    @Override
    public KnowledgeAgentResult create(CreateKnowledgeAgentCommand command) {
        presetCatalog.findSceneRulePreset(command.scenePresetCode())
                .orElseThrow(() -> new IllegalArgumentException("场景规则预设不存在: " + command.scenePresetCode()));
        if (command.chatTokenizerProfileId() != null) {
            repository.findTokenizerProfile(command.chatTokenizerProfileId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Chat Tokenizer Profile 不存在: " + command.chatTokenizerProfileId()
                    ));
        }
        command.libraryIds().forEach(libraryId -> {
            var library = repository.findLibrary(libraryId)
                    .orElseThrow(() -> new ResourceNotFoundException("知识库不存在: " + libraryId));
            if (!command.tenantId().equals(library.tenantId())) {
                throw new IllegalArgumentException("智能体不能绑定其他租户的知识库: " + libraryId);
            }
        });

        Instant now = Instant.now();
        UUID agentId = UUID.randomUUID();
        KnowledgeAgent agent = new KnowledgeAgent(
                agentId,
                command.tenantId(),
                command.name(),
                command.description(),
                AgentStatus.ACTIVE,
                now,
                now
        );
        repository.saveAgent(agent);

        SceneRulePreset scenePreset = presetCatalog.findSceneRulePreset(command.scenePresetCode()).orElseThrow();
        AgentVersion version = new AgentVersion(
                UUID.randomUUID(),
                agentId,
                1,
                AgentVersionStatus.PUBLISHED,
                command.scenePresetCode(),
                List.copyOf(command.libraryIds()),
                mergePolicy(command.routingPolicy(), mapConfig(scenePreset.config(), "routing", Map.of("mode", "selected_libraries"))),
                mergePolicy(command.retrievalPolicy(), mapConfig(scenePreset.config(), "retrieval", Map.of())),
                mergePolicy(command.answerPolicy(), mapConfig(scenePreset.config(), "answer", Map.of())),
                command.systemPrompt() == null || command.systemPrompt().isBlank()
                        ? stringConfig(scenePreset.config(), "systemPrompt", defaultSystemPrompt())
                        : command.systemPrompt(),
                command.chatTokenizerProfileId(),
                true,
                now
        );
        repository.saveAgentVersion(version);
        return ResultMapper.toKnowledgeAgentResult(agent, version);
    }

    @Override
    public KnowledgeAgentResult get(UUID agentId) {
        accessControlService.requireAgentAccess(agentId, AclPermission.READ);
        KnowledgeAgent agent = repository.findAgent(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("知识智能体不存在: " + agentId));
        AgentVersion version = repository.findPublishedAgentVersion(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("知识智能体版本不存在: " + agentId));
        return ResultMapper.toKnowledgeAgentResult(agent, version);
    }

    @Override
    public List<KnowledgeAgentResult> list(String tenantId) {
        return repository.listAgents(tenantId).stream()
                .filter(agent -> accessControlService.canAccessAgent(agent.agentId(), AclPermission.READ))
                .map(agent -> {
                    AgentVersion version = repository.findPublishedAgentVersion(agent.agentId()).orElse(null);
                    return version == null ? null : ResultMapper.toKnowledgeAgentResult(agent, version);
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Override
    public KnowledgeAgentResult createKnowledgeAgent(CreateKnowledgeAgentCommand command) {
        return create(command);
    }

    @Override
    public KnowledgeAgentResult getKnowledgeAgent(UUID agentId) {
        return get(agentId);
    }

    @Override
    public List<KnowledgeAgentResult> listKnowledgeAgents(String tenantId) {
        return list(tenantId);
    }

    private static Map<String, Object> mergePolicy(Map<String, Object> commandPolicy, Map<String, Object> defaults) {
        java.util.HashMap<String, Object> merged = new java.util.HashMap<>(defaults);
        if (commandPolicy != null) {
            merged.putAll(commandPolicy);
        }
        return Map.copyOf(merged);
    }

    private static Map<String, Object> mapConfig(Map<String, Object> config, String key, Map<String, Object> defaults) {
        Object value = config.get(key);
        if (!(value instanceof Map<?, ?> raw)) {
            return defaults;
        }
        java.util.HashMap<String, Object> result = new java.util.HashMap<>(defaults);
        raw.forEach((mapKey, mapValue) -> result.put(String.valueOf(mapKey), mapValue));
        return Map.copyOf(result);
    }

    private static String stringConfig(Map<String, Object> config, String key, String defaultValue) {
        Object value = config.get(key);
        return value == null || String.valueOf(value).isBlank() ? defaultValue : String.valueOf(value);
    }

    private static String defaultSystemPrompt() {
        return "请基于证据回答，并返回引用。";
    }
}
