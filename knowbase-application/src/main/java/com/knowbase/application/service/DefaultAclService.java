package com.knowbase.application.service;

import com.knowbase.api.command.GrantAclCommand;
import com.knowbase.api.result.AclEntryResult;
import com.knowbase.application.security.AccessControlService;
import com.knowbase.domain.model.AclEntry;
import com.knowbase.domain.repository.AccessControlRepository;
import com.knowbase.domain.security.AclPermission;
import com.knowbase.domain.security.AclPrincipalType;
import com.knowbase.domain.security.AclResourceType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class DefaultAclService {

    private final AccessControlRepository aclRepository;
    private final AccessControlService accessControlService;

    public DefaultAclService(AccessControlRepository aclRepository, AccessControlService accessControlService) {
        this.aclRepository = aclRepository;
        this.accessControlService = accessControlService;
    }

    public AclEntryResult grant(GrantAclCommand command) {
        requireAdmin(parseResourceType(command.resourceType()), command.resourceId());
        AclEntry entry = aclRepository.saveAclEntry(new AclEntry(
                UUID.randomUUID(),
                command.tenantId(),
                parseResourceType(command.resourceType()),
                command.resourceId(),
                parsePrincipalType(command.principalType()),
                command.principalId(),
                parsePermission(command.permission()),
                Instant.now()
        ));
        return toResult(entry);
    }

    public void revoke(UUID aclId) {
        AclEntry entry = aclRepository.findAclEntry(aclId)
                .orElseThrow(() -> new ResourceNotFoundException("ACL 记录不存在: " + aclId));
        requireAdmin(entry.resourceType(), entry.resourceId());
        aclRepository.deleteAclEntry(aclId);
    }

    public List<AclEntryResult> list(String tenantId, String resourceType, UUID resourceId) {
        AclResourceType parsedType = parseResourceType(resourceType);
        requireRead(parsedType, resourceId);
        return aclRepository.listAclEntries(tenantId, parsedType, resourceId).stream()
                .map(DefaultAclService::toResult)
                .toList();
    }

    private void requireAdmin(AclResourceType resourceType, UUID resourceId) {
        switch (resourceType) {
            case LIBRARY -> accessControlService.requireLibraryAccess(resourceId, AclPermission.ADMIN);
            case AGENT -> accessControlService.requireAgentAccess(resourceId, AclPermission.ADMIN);
            case DOCUMENT -> accessControlService.requireDocumentAccess(resourceId, AclPermission.ADMIN);
        }
    }

    private void requireRead(AclResourceType resourceType, UUID resourceId) {
        switch (resourceType) {
            case LIBRARY -> accessControlService.requireLibraryAccess(resourceId, AclPermission.READ);
            case AGENT -> accessControlService.requireAgentAccess(resourceId, AclPermission.READ);
            case DOCUMENT -> accessControlService.requireDocumentAccess(resourceId, AclPermission.READ);
        }
    }

    private static AclEntryResult toResult(AclEntry entry) {
        return new AclEntryResult(
                entry.aclId(),
                entry.tenantId(),
                entry.resourceType().name(),
                entry.resourceId(),
                entry.principalType().name(),
                entry.principalId(),
                entry.permission().name(),
                entry.createdAt()
        );
    }

    private static AclResourceType parseResourceType(String value) {
        return AclResourceType.valueOf(value.trim().toUpperCase());
    }

    private static AclPrincipalType parsePrincipalType(String value) {
        return AclPrincipalType.valueOf(value.trim().toUpperCase());
    }

    private static AclPermission parsePermission(String value) {
        return AclPermission.valueOf(value.trim().toUpperCase());
    }
}
