package com.knowbase.application.service;

import com.knowbase.domain.model.AclEntry;
import com.knowbase.domain.repository.AccessControlRepository;
import com.knowbase.domain.security.AclResourceType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryAccessControlRepository implements AccessControlRepository {

    private final ConcurrentMap<UUID, AclEntry> entries = new ConcurrentHashMap<>();

    @Override
    public AclEntry saveAclEntry(AclEntry entry) {
        entries.put(entry.aclId(), entry);
        return entry;
    }

    @Override
    public void deleteAclEntry(UUID aclId) {
        entries.remove(aclId);
    }

    @Override
    public Optional<AclEntry> findAclEntry(UUID aclId) {
        return Optional.ofNullable(entries.get(aclId));
    }

    @Override
    public List<AclEntry> listAclEntries(String tenantId, AclResourceType resourceType, UUID resourceId) {
        return entries.values().stream()
                .filter(entry -> tenantId.equals(entry.tenantId()))
                .filter(entry -> entry.resourceType() == resourceType)
                .filter(entry -> entry.resourceId().equals(resourceId))
                .toList();
    }
}
