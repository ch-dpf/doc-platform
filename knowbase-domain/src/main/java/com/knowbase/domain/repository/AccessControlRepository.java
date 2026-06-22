package com.knowbase.domain.repository;

import com.knowbase.domain.model.AclEntry;
import com.knowbase.domain.security.AclResourceType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccessControlRepository {

    AclEntry saveAclEntry(AclEntry entry);

    void deleteAclEntry(UUID aclId);

    Optional<AclEntry> findAclEntry(UUID aclId);

    List<AclEntry> listAclEntries(String tenantId, AclResourceType resourceType, UUID resourceId);
}
