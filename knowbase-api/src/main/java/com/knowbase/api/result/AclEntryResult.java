package com.knowbase.api.result;

import java.time.Instant;
import java.util.UUID;

public record AclEntryResult(
        UUID aclId,
        String tenantId,
        String resourceType,
        UUID resourceId,
        String principalType,
        String principalId,
        String permission,
        Instant createdAt
) {
}
