package com.knowbase.application.security;

import com.knowbase.domain.model.AclEntry;
import com.knowbase.domain.model.KnowledgeAgent;
import com.knowbase.domain.model.KnowledgeDocument;
import com.knowbase.domain.model.KnowledgeLibrary;
import com.knowbase.domain.repository.AccessControlRepository;
import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.domain.security.AclPermission;
import com.knowbase.domain.security.AclPrincipalType;
import com.knowbase.domain.security.AclResourceType;

import java.util.List;
import java.util.UUID;

public final class AccessControlService {

    private final AccessControlRepository aclRepository;
    private final KnowbaseRepository knowbaseRepository;
    private final boolean aclEnabled;

    public AccessControlService(
            AccessControlRepository aclRepository,
            KnowbaseRepository knowbaseRepository,
            boolean aclEnabled
    ) {
        this.aclRepository = aclRepository;
        this.knowbaseRepository = knowbaseRepository;
        this.aclEnabled = aclEnabled;
    }

    public void requireLibraryAccess(UUID libraryId, AclPermission permission) {
        if (!canAccessLibrary(libraryId, permission)) {
            throw new AccessDeniedException("无权访问知识库: " + libraryId);
        }
    }

    public void requireAgentAccess(UUID agentId, AclPermission permission) {
        if (!canAccessAgent(agentId, permission)) {
            throw new AccessDeniedException("无权访问智能体: " + agentId);
        }
    }

    public void requireDocumentAccess(UUID documentId, AclPermission permission) {
        if (!canAccessDocument(documentId, permission)) {
            throw new AccessDeniedException("无权访问文档: " + documentId);
        }
    }

    public boolean canAccessLibrary(UUID libraryId, AclPermission permission) {
        KnowledgeLibrary library = knowbaseRepository.findLibrary(libraryId).orElse(null);
        if (library == null) {
            return false;
        }
        return evaluate(library.tenantId(), AclResourceType.LIBRARY, libraryId, permission);
    }

    public boolean canAccessAgent(UUID agentId, AclPermission permission) {
        KnowledgeAgent agent = knowbaseRepository.findAgent(agentId).orElse(null);
        if (agent == null) {
            return false;
        }
        return evaluate(agent.tenantId(), AclResourceType.AGENT, agentId, permission);
    }

    public boolean canAccessDocument(UUID documentId, AclPermission permission) {
        KnowledgeDocument document = knowbaseRepository.findDocument(documentId).orElse(null);
        if (document == null) {
            return false;
        }
        KnowledgeLibrary library = knowbaseRepository.findLibrary(document.libraryId()).orElse(null);
        if (library == null) {
            return false;
        }
        if (!evaluate(library.tenantId(), AclResourceType.DOCUMENT, documentId, permission)) {
            return false;
        }
        return canAccessLibrary(document.libraryId(), permission);
    }

    public List<KnowledgeLibrary> filterLibraries(List<KnowledgeLibrary> libraries, AclPermission permission) {
        return libraries.stream()
                .filter(library -> evaluate(library.tenantId(), AclResourceType.LIBRARY, library.libraryId(), permission))
                .toList();
    }

    private boolean evaluate(String tenantId, AclResourceType resourceType, UUID resourceId, AclPermission permission) {
        KnowbaseRequestContext.Snapshot context = KnowbaseRequestContext.get();
        if (context != null && !tenantId.equals(context.tenantId())) {
            return false;
        }
        if (!aclEnabled) {
            return true;
        }
        if (context != null && context.roles().contains("admin")) {
            return true;
        }
        List<AclEntry> entries = aclRepository.listAclEntries(tenantId, resourceType, resourceId);
        if (entries.isEmpty()) {
            return context != null;
        }
        if (context == null) {
            return false;
        }
        return entries.stream().anyMatch(entry -> matchesPrincipal(entry, context) && entry.permission().satisfies(permission));
    }

    private static boolean matchesPrincipal(AclEntry entry, KnowbaseRequestContext.Snapshot context) {
        if (entry.principalType() == AclPrincipalType.USER) {
            return entry.principalId().equals(context.userId());
        }
        return context.roles().contains(entry.principalId());
    }
}
