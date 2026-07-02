package com.knowbase.autoconfigure;

import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

/**
 * Runs KnowBase Flyway migrations with optional PostgreSQL schema isolation.
 */
public final class KnowbaseFlywaySupport {

    private static final Logger log = LoggerFactory.getLogger(KnowbaseFlywaySupport.class);

    private KnowbaseFlywaySupport() {
    }

    public static void migrateIfEnabled(DataSource dataSource, KnowbaseProperties properties) {
        if (properties == null || !properties.getFlyway().isEnabled()) {
            return;
        }

        KnowbaseProperties.Flyway flywayProperties = properties.getFlyway();
        ensureSchemaExists(dataSource, resolveSchema(properties));
        org.flywaydb.core.Flyway flyway = configure(dataSource, properties).load();

        if (shouldBaselineExistingSchema(dataSource, properties, flywayProperties, flyway)) {
            String baselineVersion = resolveLatestVersion(flyway);
            log.warn(
                    "Detected existing KnowBase schema (table={}) without completed Flyway history; baselining {} at V{}",
                    qualifiedProbeTable(properties, flywayProperties.getExistingSchemaProbeTable()),
                    qualifiedHistoryTable(properties, flywayProperties.getTable()),
                    baselineVersion
            );
            resetHistoryTable(dataSource, properties, flywayProperties.getTable());
            configure(dataSource, properties)
                    .baselineVersion(baselineVersion)
                    .baselineDescription("Pre-existing knowbase schema")
                    .load()
                    .baseline();
            flyway = configure(dataSource, properties).load();
        }

        log.info(
                "Running KnowBase Flyway migrations (schema: {}, history table: {})",
                schemaLabel(properties),
                qualifiedHistoryTable(properties, flywayProperties.getTable())
        );
        flyway.migrate();
    }

    public static FluentConfiguration configure(DataSource dataSource, KnowbaseProperties properties) {
        KnowbaseProperties.Flyway flywayProperties = properties.getFlyway();
        FluentConfiguration configuration = org.flywaydb.core.Flyway.configure()
                .dataSource(dataSource)
                .locations(flywayProperties.getLocations())
                .table(flywayProperties.getTable())
                .baselineOnMigrate(flywayProperties.isBaselineOnMigrate())
                .baselineVersion(flywayProperties.getBaselineVersion())
                .outOfOrder(flywayProperties.isOutOfOrder())
                .validateOnMigrate(flywayProperties.isValidateOnMigrate())
                .cleanDisabled(flywayProperties.isCleanDisabled());

        String schema = resolveSchema(properties);
        if (schema != null) {
            configuration.schemas(schema).defaultSchema(schema);
        }
        return configuration;
    }

    private static boolean shouldBaselineExistingSchema(
            DataSource dataSource,
            KnowbaseProperties properties,
            KnowbaseProperties.Flyway flywayProperties,
            org.flywaydb.core.Flyway flyway
    ) {
        if (!flywayProperties.isBaselineExistingSchema()) {
            return false;
        }
        if (!tableExists(dataSource, properties, flywayProperties.getExistingSchemaProbeTable())) {
            return false;
        }
        MigrationInfo current = flyway.info().current();
        if (current == null || current.getVersion() == null) {
            return true;
        }
        MigrationVersion latest = resolveLatestVersionObject(flyway);
        return current.getVersion().compareTo(latest) < 0;
    }

    private static void ensureSchemaExists(DataSource dataSource, String schema) {
        if (schema == null) {
            return;
        }
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS " + quoteIdentifier(schema));
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to create KnowBase schema: " + schema, exception);
        }
    }

    private static void resetHistoryTable(DataSource dataSource, KnowbaseProperties properties, String historyTable) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS " + qualifiedHistoryTable(properties, historyTable));
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to reset KnowBase Flyway history table: " + historyTable, exception);
        }
    }

    private static String resolveLatestVersion(org.flywaydb.core.Flyway flyway) {
        return resolveLatestVersionObject(flyway).toString();
    }

    private static MigrationVersion resolveLatestVersionObject(org.flywaydb.core.Flyway flyway) {
        return Arrays.stream(flyway.info().all())
                .map(MigrationInfo::getVersion)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(MigrationVersion.fromVersion("1"));
    }

    private static boolean tableExists(DataSource dataSource, KnowbaseProperties properties, String tableName) {
        String schema = metadataSchema(properties);
        try (Connection connection = dataSource.getConnection();
             ResultSet resultSet = connection.getMetaData().getTables(null, schema, tableName, new String[]{"TABLE"})) {
            return resultSet.next();
        } catch (SQLException exception) {
            log.warn("Unable to check whether table {}.{} exists", schema, tableName, exception);
            return false;
        }
    }

    private static String resolveSchema(KnowbaseProperties properties) {
        if (properties == null || properties.getPersistence() == null) {
            return null;
        }
        String schema = properties.getPersistence().getSchema();
        if (schema == null || schema.isBlank()) {
            return null;
        }
        return schema.trim();
    }

    private static String metadataSchema(KnowbaseProperties properties) {
        String schema = resolveSchema(properties);
        return schema == null ? "public" : schema;
    }

    private static String qualifiedHistoryTable(KnowbaseProperties properties, String tableName) {
        String schema = resolveSchema(properties);
        return schema == null ? tableName : schema + "." + tableName;
    }

    private static String qualifiedProbeTable(KnowbaseProperties properties, String tableName) {
        return qualifiedHistoryTable(properties, tableName);
    }

    private static String schemaLabel(KnowbaseProperties properties) {
        String schema = resolveSchema(properties);
        return schema == null ? "default" : schema;
    }

    private static String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
