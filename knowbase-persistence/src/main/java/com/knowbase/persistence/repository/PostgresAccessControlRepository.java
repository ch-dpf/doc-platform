package com.knowbase.persistence.repository;

import com.knowbase.domain.model.AclEntry;
import com.knowbase.domain.repository.AccessControlRepository;
import com.knowbase.domain.security.AclPermission;
import com.knowbase.domain.security.AclPrincipalType;
import com.knowbase.domain.security.AclResourceType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class PostgresAccessControlRepository implements AccessControlRepository {

    private static final RowMapper<AclEntry> ROW_MAPPER = (rs, rowNum) -> new AclEntry(
            rs.getObject("acl_id", UUID.class),
            rs.getString("tenant_id"),
            AclResourceType.valueOf(rs.getString("resource_type")),
            rs.getObject("resource_id", UUID.class),
            AclPrincipalType.valueOf(rs.getString("principal_type")),
            rs.getString("principal_id"),
            AclPermission.valueOf(rs.getString("permission")),
            rs.getTimestamp("created_at").toInstant()
    );

    private final JdbcTemplate jdbcTemplate;

    public PostgresAccessControlRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AclEntry saveAclEntry(AclEntry entry) {
        jdbcTemplate.update(
                """
                        INSERT INTO kb_acl_entry (acl_id, tenant_id, resource_type, resource_id, principal_type, principal_id, permission, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (acl_id) DO UPDATE SET
                            permission = EXCLUDED.permission
                        """,
                entry.aclId(),
                entry.tenantId(),
                entry.resourceType().name(),
                entry.resourceId(),
                entry.principalType().name(),
                entry.principalId(),
                entry.permission().name(),
                Timestamp.from(entry.createdAt())
        );
        return entry;
    }

    @Override
    public void deleteAclEntry(UUID aclId) {
        jdbcTemplate.update("DELETE FROM kb_acl_entry WHERE acl_id = ?", aclId);
    }

    @Override
    public Optional<AclEntry> findAclEntry(UUID aclId) {
        List<AclEntry> rows = jdbcTemplate.query(
                "SELECT * FROM kb_acl_entry WHERE acl_id = ?",
                ROW_MAPPER,
                aclId
        );
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
    }

    @Override
    public List<AclEntry> listAclEntries(String tenantId, AclResourceType resourceType, UUID resourceId) {
        return jdbcTemplate.query(
                """
                        SELECT * FROM kb_acl_entry
                        WHERE tenant_id = ? AND resource_type = ? AND resource_id = ?
                        ORDER BY created_at ASC
                        """,
                ROW_MAPPER,
                tenantId,
                resourceType.name(),
                resourceId
        );
    }
}
