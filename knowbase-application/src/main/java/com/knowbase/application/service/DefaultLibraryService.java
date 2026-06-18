package com.knowbase.application.service;

import com.knowbase.api.command.CreateLibraryCommand;
import com.knowbase.api.command.DocumentProfileCommand;
import com.knowbase.api.command.LibraryProfileCommand;
import com.knowbase.api.facade.KnowbaseLibraryFacade;
import com.knowbase.api.result.LibraryResult;
import com.knowbase.application.mapper.ResultMapper;
import com.knowbase.application.security.AccessControlService;
import com.knowbase.application.usecase.CreateLibraryUseCase;
import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.model.KnowledgeLibrary;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.domain.model.LibraryTypePreset;
import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.domain.security.AclPermission;
import com.knowbase.domain.status.ContentFamily;
import com.knowbase.domain.status.LibraryStatus;
import com.knowbase.preset.PresetCatalog;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DefaultLibraryService implements CreateLibraryUseCase, KnowbaseLibraryFacade {

    private final KnowbaseRepository repository;
    private final PresetCatalog presetCatalog;
    private final AccessControlService accessControlService;

    public DefaultLibraryService(
            KnowbaseRepository repository,
            PresetCatalog presetCatalog,
            AccessControlService accessControlService
    ) {
        this.repository = repository;
        this.presetCatalog = presetCatalog;
        this.accessControlService = accessControlService;
    }

    @Override
    public LibraryResult create(CreateLibraryCommand command) {
        LibraryTypePreset preset = presetCatalog.findLibraryTypePreset(command.libraryTypePresetCode())
                .orElseThrow(() -> new IllegalArgumentException("库类型预设不存在: " + command.libraryTypePresetCode()));

        Instant now = Instant.now();
        UUID libraryId = UUID.randomUUID();
        KnowledgeLibrary library = new KnowledgeLibrary(
                libraryId,
                command.tenantId(),
                command.name(),
                command.description(),
                LibraryStatus.ACTIVE,
                command.libraryTypePresetCode(),
                command.tags() == null ? List.of() : List.copyOf(command.tags()),
                now,
                now
        );
        repository.saveLibrary(library);
        repository.saveLibraryProfile(buildLibraryProfile(libraryId, command.profile(), preset.config(), now));
        saveDocumentProfiles(libraryId, command.documentProfiles(), preset.config());
        return ResultMapper.toLibraryResult(library);
    }

    @Override
    public LibraryResult get(UUID libraryId) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.READ);
        return repository.findLibrary(libraryId)
                .map(ResultMapper::toLibraryResult)
                .orElseThrow(() -> new ResourceNotFoundException("知识库不存在: " + libraryId));
    }

    @Override
    public List<LibraryResult> list(String tenantId) {
        return accessControlService.filterLibraries(repository.listLibraries(tenantId), AclPermission.READ).stream()
                .map(ResultMapper::toLibraryResult)
                .toList();
    }

    @Override
    public LibraryResult createLibrary(CreateLibraryCommand command) {
        return create(command);
    }

    @Override
    public LibraryResult getLibrary(UUID libraryId) {
        return get(libraryId);
    }

    @Override
    public List<LibraryResult> listLibraries(String tenantId) {
        return list(tenantId);
    }

    private void saveDocumentProfiles(UUID libraryId, List<DocumentProfileCommand> commands, Map<String, Object> presetConfig) {
        if (commands == null || commands.isEmpty()) {
            defaultDocumentProfiles(libraryId, presetConfig).forEach(repository::saveDocumentProfile);
            return;
        }
        for (DocumentProfileCommand command : commands) {
            repository.saveDocumentProfile(new DocumentProfile(
                    UUID.randomUUID(),
                    libraryId,
                    documentProfileCode(command),
                    parseContentFamily(command.contentFamily()),
                    command.parserCode(),
                    command.chunkingStrategy(),
                    command.tokenizerProfileId(),
                    command.metadataSchema() == null ? Map.of() : command.metadataSchema(),
                    command.options() == null ? Map.of() : command.options(),
                    true
            ));
        }
    }

    private static ContentFamily parseContentFamily(String value) {
        return ContentFamily.valueOf(value.trim().toUpperCase());
    }

    private static String documentProfileCode(DocumentProfileCommand command) {
        if (command.code() != null && !command.code().isBlank()) {
            return command.code().trim();
        }
        return parseContentFamily(command.contentFamily()).name().toLowerCase();
    }

    private static List<DocumentProfile> defaultDocumentProfiles(UUID libraryId, Map<String, Object> presetConfig) {
        Object configured = presetConfig.get("documentProfiles");
        if (configured instanceof List<?> profiles && !profiles.isEmpty()) {
            return profiles.stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .map(profile -> defaultDocumentProfile(libraryId, profile))
                    .toList();
        }
        return List.of(defaultDocumentProfile(libraryId, Map.of(
                "code", "default_markdown",
                "contentFamily", "RICH_TEXT",
                "parserCode", "text",
                "chunkingStrategy", "structure_token_window"
        )));
    }

    private static DocumentProfile defaultDocumentProfile(UUID libraryId, Map<?, ?> profile) {
        return new DocumentProfile(
                UUID.randomUUID(),
                libraryId,
                stringValue(profile, "code", "default_markdown"),
                parseContentFamily(stringValue(profile, "contentFamily", "RICH_TEXT")),
                stringValue(profile, "parserCode", "tika"),
                stringValue(profile, "chunkingStrategy", "structure_token_window"),
                null,
                mapValue(profile, "metadataSchema"),
                mapValue(profile, "options"),
                true
        );
    }

    private static LibraryProfile buildLibraryProfile(
            UUID libraryId,
            LibraryProfileCommand command,
            Map<String, Object> presetConfig,
            Instant createdAt
    ) {
        String embeddingProvider = command == null
                ? String.valueOf(presetConfig.getOrDefault("embeddingProvider", "ollama"))
                : command.embeddingProvider();
        String embeddingModel = command == null
                ? String.valueOf(presetConfig.getOrDefault("embeddingModel", "bge-m3"))
                : command.embeddingModel();
        int embeddingDimension = command == null
                ? ((Number) presetConfig.getOrDefault("embeddingDimension", 1024)).intValue()
                : command.embeddingDimension();
        int chunkMaxTokens = command == null
                ? ((Number) presetConfig.getOrDefault("chunkMaxTokens", 512)).intValue()
                : command.chunkMaxTokens();
        int chunkOverlapTokens = command == null
                ? ((Number) presetConfig.getOrDefault("chunkOverlapTokens", 80)).intValue()
                : command.chunkOverlapTokens();
        int retrievalTopK = command == null
                ? ((Number) presetConfig.getOrDefault("retrievalTopK", 8)).intValue()
                : command.retrievalTopK();
        return new LibraryProfile(
                UUID.randomUUID(),
                libraryId,
                1,
                embeddingProvider,
                embeddingModel,
                embeddingDimension,
                command == null ? null : command.embeddingTokenizerProfileId(),
                chunkMaxTokens,
                chunkOverlapTokens,
                retrievalTopK,
                command == null || command.options() == null ? Map.of() : command.options(),
                createdAt
        );
    }

    private static String stringValue(Map<?, ?> values, String key, String defaultValue) {
        Object value = values.get(key);
        return value == null || String.valueOf(value).isBlank() ? defaultValue : String.valueOf(value);
    }

    private static Map<String, Object> mapValue(Map<?, ?> values, String key) {
        Object value = values.get(key);
        if (!(value instanceof Map<?, ?> raw)) {
            return Map.of();
        }
        java.util.HashMap<String, Object> result = new java.util.HashMap<>();
        raw.forEach((mapKey, mapValue) -> result.put(String.valueOf(mapKey), mapValue));
        return Map.copyOf(result);
    }
}
