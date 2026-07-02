package com.knowbase.domain.model;

import com.knowbase.domain.security.AclPermission;
import com.knowbase.domain.security.AclPrincipalType;
import com.knowbase.domain.security.AclResourceType;

import java.time.Instant;
import java.util.UUID;

public record AclEntry(
        UUID aclId,
        String tenantId,
        AclResourceType resourceType,
        UUID resourceId,
        AclPrincipalType principalType,
        String principalId,
        AclPermission permission,
        Instant createdAt
) {
}
