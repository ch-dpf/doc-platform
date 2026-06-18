package com.knowbase.application.agent;

import com.knowbase.agent.LibraryRouter;
import com.knowbase.agent.RouteRequest;
import com.knowbase.application.security.AccessControlService;
import com.knowbase.domain.security.AclPermission;

import java.util.List;
import java.util.UUID;

public final class AclAwareLibraryRouter implements LibraryRouter {

    private final LibraryRouter delegate;
    private final AccessControlService accessControlService;

    public AclAwareLibraryRouter(LibraryRouter delegate, AccessControlService accessControlService) {
        this.delegate = delegate;
        this.accessControlService = accessControlService;
    }

    @Override
    public List<UUID> route(RouteRequest request) {
        List<UUID> accessible = request.candidateLibraryIds().stream()
                .filter(libraryId -> accessControlService.canAccessLibrary(libraryId, AclPermission.READ))
                .toList();
        if (accessible.isEmpty()) {
            return List.of();
        }
        return delegate.route(new RouteRequest(
                request.agentVersionId(),
                request.question(),
                accessible,
                request.routingPolicy()
        ));
    }
}
