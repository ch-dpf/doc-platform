package com.knowbase.persistence.jdbc;

/**
 * Marker for {@link JdbcTemplate} wrappers that qualify {@code kb_*} table names and
 * set {@code search_path} for connection callbacks when a schema is configured.
 */
public interface KnowbaseSchemaJdbcTemplate {
}
