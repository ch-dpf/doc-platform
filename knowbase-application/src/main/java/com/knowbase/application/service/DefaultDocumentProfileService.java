package com.knowbase.application.service;

import com.knowbase.api.command.CreateDocumentProfileCommand;
import com.knowbase.api.command.UpdateDocumentProfileCommand;
import com.knowbase.api.result.DocumentProfileResult;
import com.knowbase.application.mapper.ResultMapper;
import com.knowbase.application.security.AccessControlService;
import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.domain.security.AclPermission;
import com.knowbase.domain.status.ContentFamily;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DefaultDocumentProfileService {

    private final KnowbaseRepository repository;
    private final AccessControlService accessControlService;

    public DefaultDocumentProfileService(KnowbaseRepository repository, AccessControlService accessControlService) {
        this.repository = repository;
        this.accessControlService = accessControlService;
    }

    public List<DocumentProfileResult> list(UUID libraryId) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.READ);
        requireLibrary(libraryId);
        return repository.listDocumentProfiles(libraryId).stream()
                .map(ResultMapper::toDocumentProfileResult)
                .toList();
    }

    public DocumentProfileResult get(UUID libraryId, String code) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.READ);
        return ResultMapper.toDocumentProfileResult(requireProfile(libraryId, code));
    }

    public DocumentProfileResult create(UUID libraryId, CreateDocumentProfileCommand command) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.WRITE);
        requireLibrary(libraryId);
        String code = command.code().trim();
        if (repository.findDocumentProfile(libraryId, code).isPresent()) {
            throw new IllegalArgumentException("Document Profile 已存在: " + code);
        }
        DocumentProfile profile = new DocumentProfile(
                UUID.randomUUID(),
                libraryId,
                code,
                parseContentFamily(command.contentFamily()),
                command.parserCode(),
                command.chunkingStrategy(),
                command.tokenizerProfileId(),
                command.metadataSchema() == null ? Map.of() : command.metadataSchema(),
                command.options() == null ? Map.of() : command.options(),
                command.enabled() == null || command.enabled()
        );
        return ResultMapper.toDocumentProfileResult(repository.saveDocumentProfile(profile));
    }

    public DocumentProfileResult update(UUID libraryId, String code, UpdateDocumentProfileCommand command) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.WRITE);
        DocumentProfile existing = requireProfile(libraryId, code);
        DocumentProfile updated = new DocumentProfile(
                existing.documentProfileId(),
                existing.libraryId(),
                existing.code(),
                command.contentFamily() == null ? existing.contentFamily() : parseContentFamily(command.contentFamily()),
                command.parserCode() == null ? existing.parserCode() : command.parserCode(),
                command.chunkingStrategy() == null ? existing.chunkingStrategy() : command.chunkingStrategy(),
                command.tokenizerProfileId() == null ? existing.tokenizerProfileId() : command.tokenizerProfileId(),
                command.metadataSchema() == null ? existing.metadataSchema() : command.metadataSchema(),
                command.options() == null ? existing.options() : command.options(),
                command.enabled() == null ? existing.enabled() : command.enabled()
        );
        return ResultMapper.toDocumentProfileResult(repository.saveDocumentProfile(updated));
    }

    public void delete(UUID libraryId, String code) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.WRITE);
        requireProfile(libraryId, code);
        repository.deleteDocumentProfile(libraryId, code);
    }

    private void requireLibrary(UUID libraryId) {
        repository.findLibrary(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("知识库不存在: " + libraryId));
    }

    private DocumentProfile requireProfile(UUID libraryId, String code) {
        return repository.findDocumentProfile(libraryId, code)
                .orElseThrow(() -> new ResourceNotFoundException("Document Profile 不存在: " + code));
    }

    private static ContentFamily parseContentFamily(String value) {
        return ContentFamily.valueOf(value.trim().toUpperCase());
    }
}
