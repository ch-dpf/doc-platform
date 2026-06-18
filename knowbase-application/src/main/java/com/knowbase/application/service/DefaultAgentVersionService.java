package com.knowbase.application.service;

import com.knowbase.api.command.CreateAgentVersionCommand;
import com.knowbase.api.result.AgentVersionResult;
import com.knowbase.application.security.AccessControlService;
import com.knowbase.domain.model.AgentVersion;
import com.knowbase.domain.model.KnowledgeAgent;
import com.knowbase.domain.model.SceneRulePreset;
import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.domain.security.AclPermission;
import com.knowbase.domain.status.AgentVersionStatus;
import com.knowbase.preset.PresetCatalog;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DefaultAgentVersionService {

    private final KnowbaseRepository repository;
    private final PresetCatalog presetCatalog;
    private final AccessControlService accessControlService;

    public DefaultAgentVersionService(
            KnowbaseRepository repository,
            PresetCatalog presetCatalog,
            AccessControlService accessControlService
    ) {
        this.repository = repository;
        this.presetCatalog = presetCatalog;
        this.accessControlService = accessControlService;
    }

    public List<AgentVersionResult> list(UUID agentId) {
        accessControlService.requireAgentAccess(agentId, AclPermission.READ);
        repository.findAgent(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("知识智能体不存在: " + agentId));
        return repository.listAgentVersions(agentId).stream()
                .map(DefaultAgentVersionService::toResult)
                .toList();
    }

    public AgentVersionResult get(UUID agentId, UUID agentVersionId) {
        accessControlService.requireAgentAccess(agentId, AclPermission.READ);
        return toResult(findVersion(agentId, agentVersionId));
    }

    public AgentVersionResult create(UUID agentId, CreateAgentVersionCommand command) {
        accessControlService.requireAgentAccess(agentId, AclPermission.WRITE);
        KnowledgeAgent agent = repository.findAgent(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("知识智能体不存在: " + agentId));
        SceneRulePreset scenePreset = presetCatalog.findSceneRulePreset(command.scenePresetCode())
                .orElseThrow(() -> new IllegalArgumentException("场景规则预设不存在: " + command.scenePresetCode()));
        validateLibraries(agent.tenantId(), command.libraryIds());
        int nextVersion = repository.listAgentVersions(agentId).stream()
                .mapToInt(AgentVersion::version)
                .max()
                .orElse(0) + 1;
        AgentVersion version = new AgentVersion(
                UUID.randomUUID(),
                agentId,
                nextVersion,
                AgentVersionStatus.DRAFT,
                command.scenePresetCode(),
                List.copyOf(command.libraryIds()),
                mergePolicy(command.routingPolicy(), mapConfig(scenePreset.config(), "routing", Map.of("mode", "selected_libraries"))),
                mergePolicy(command.retrievalPolicy(), mapConfig(scenePreset.config(), "retrieval", Map.of())),
                mergePolicy(command.answerPolicy(), mapConfig(scenePreset.config(), "answer", Map.of())),
                command.systemPrompt() == null || command.systemPrompt().isBlank()
                        ? stringConfig(scenePreset.config(), "systemPrompt", "请基于证据回答，并返回引用。")
                        : command.systemPrompt(),
                command.chatTokenizerProfileId(),
                false,
                Instant.now()
        );
        return toResult(repository.saveAgentVersion(version));
    }

    public AgentVersionResult publish(UUID agentId, UUID agentVersionId) {
        accessControlService.requireAgentAccess(agentId, AclPermission.WRITE);
        AgentVersion version = findVersion(agentId, agentVersionId);
        for (AgentVersion existing : repository.listAgentVersions(agentId)) {
            if (existing.published()) {
                repository.saveAgentVersion(new AgentVersion(
                        existing.agentVersionId(),
                        existing.agentId(),
                        existing.version(),
                        AgentVersionStatus.DISABLED,
                        existing.scenePresetCode(),
                        existing.libraryIds(),
                        existing.routingPolicy(),
                        existing.retrievalPolicy(),
                        existing.answerPolicy(),
                        existing.systemPrompt(),
                        existing.chatTokenizerProfileId(),
                        false,
                        existing.createdAt()
                ));
            }
        }
        AgentVersion published = new AgentVersion(
                version.agentVersionId(),
                version.agentId(),
                version.version(),
                AgentVersionStatus.PUBLISHED,
                version.scenePresetCode(),
                version.libraryIds(),
                version.routingPolicy(),
                version.retrievalPolicy(),
                version.answerPolicy(),
                version.systemPrompt(),
                version.chatTokenizerProfileId(),
                true,
                version.createdAt()
        );
        return toResult(repository.saveAgentVersion(published));
    }

    public AgentVersionResult disable(UUID agentId, UUID agentVersionId) {
        accessControlService.requireAgentAccess(agentId, AclPermission.WRITE);
        AgentVersion version = findVersion(agentId, agentVersionId);
        AgentVersion disabled = new AgentVersion(
                version.agentVersionId(),
                version.agentId(),
                version.version(),
                AgentVersionStatus.DISABLED,
                version.scenePresetCode(),
                version.libraryIds(),
                version.routingPolicy(),
                version.retrievalPolicy(),
                version.answerPolicy(),
                version.systemPrompt(),
                version.chatTokenizerProfileId(),
                false,
                version.createdAt()
        );
        return toResult(repository.saveAgentVersion(disabled));
    }

    private AgentVersion findVersion(UUID agentId, UUID agentVersionId) {
        return repository.findAgentVersion(agentVersionId)
                .filter(version -> version.agentId().equals(agentId))
                .orElseThrow(() -> new ResourceNotFoundException("智能体版本不存在: " + agentVersionId));
    }

    private void validateLibraries(String tenantId, List<UUID> libraryIds) {
        libraryIds.forEach(libraryId -> {
            var library = repository.findLibrary(libraryId)
                    .orElseThrow(() -> new ResourceNotFoundException("知识库不存在: " + libraryId));
            if (!tenantId.equals(library.tenantId())) {
                throw new IllegalArgumentException("智能体不能绑定其他租户的知识库: " + libraryId);
            }
        });
    }

    private static AgentVersionResult toResult(AgentVersion version) {
        return new AgentVersionResult(
                version.agentVersionId(),
                version.agentId(),
                version.version(),
                version.status().name(),
                version.scenePresetCode(),
                version.libraryIds(),
                version.routingPolicy(),
                version.retrievalPolicy(),
                version.answerPolicy(),
                version.systemPrompt(),
                version.chatTokenizerProfileId(),
                version.published(),
                version.createdAt()
        );
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
}
