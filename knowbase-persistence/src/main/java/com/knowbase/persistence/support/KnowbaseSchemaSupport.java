package com.knowbase.persistence.support;

import java.util.regex.Pattern;

/**
 * Optional PostgreSQL schema namespace for KnowBase tables (e.g. {@code knowbase} when embedded in a host app).
 */
public final class KnowbaseSchemaSupport {

    private static final Pattern KB_TABLE = Pattern.compile("(?<!\\w\\.)\\b(kb_[a-z_]+)\\b");

    private final String schema;

    private KnowbaseSchemaSupport(String schema) {
        this.schema = normalize(schema);
    }

    public static KnowbaseSchemaSupport of(String schema) {
        return new KnowbaseSchemaSupport(schema);
    }

    public boolean hasSchema() {
        return schema != null;
    }

    public String schema() {
        return schema;
    }

    /** Schema name used for JDBC metadata lookups ({@code public} when unset). */
    public String metadataSchema() {
        return schema == null ? "public" : schema;
    }

    public String table(String bareName) {
        return hasSchema() ? schema + "." + bareName : bareName;
    }

    public String qualifySql(String sql) {
        if (!hasSchema() || sql == null || sql.isBlank()) {
            return sql;
        }
        return KB_TABLE.matcher(sql).replaceAll(schema + ".$1");
    }

    private static String normalize(String schema) {
        if (schema == null || schema.isBlank()) {
            return null;
        }
        return schema.trim();
    }
}
