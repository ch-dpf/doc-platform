package com.knowbase.persistence.repository;

import com.knowbase.domain.model.LibraryTypePreset;
import com.knowbase.domain.model.SceneRulePreset;
import com.knowbase.domain.repository.PresetRepository;
import com.knowbase.persistence.support.JsonSupport;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class PostgresPresetRepository implements PresetRepository {

    private final JdbcTemplate jdbcTemplate;

    public PostgresPresetRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public LibraryTypePreset saveLibraryTypePreset(LibraryTypePreset preset) {
        Instant now = Instant.now();
        jdbcTemplate.update(
                """
                INSERT INTO kb_library_type_preset (
                    preset_id, tenant_id, code, name, description, config_json, built_in, enabled, created_at
                ) VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?)
                ON CONFLICT (tenant_id, code) DO UPDATE SET
                    name = EXCLUDED.name,
                    description = EXCLUDED.description,
                    config_json = EXCLUDED.config_json,
                    built_in = EXCLUDED.built_in,
                    enabled = EXCLUDED.enabled
                """,
                preset.presetId() == null ? UUID.randomUUID() : preset.presetId(),
                preset.tenantId(),
                preset.code(),
                preset.name(),
                preset.description(),
                JsonSupport.write(preset.config()),
                preset.builtIn(),
                preset.enabled(),
                Timestamp.from(now)
        );
        return findLibraryTypePreset(preset.tenantId(), preset.code()).orElse(preset);
    }

    @Override
    public SceneRulePreset saveSceneRulePreset(SceneRulePreset preset) {
        Instant now = Instant.now();
        jdbcTemplate.update(
                """
                INSERT INTO kb_scene_rule_preset (
                    preset_id, tenant_id, code, name, description, config_json, built_in, enabled, created_at
                ) VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?)
                ON CONFLICT (tenant_id, code) DO UPDATE SET
                    name = EXCLUDED.name,
                    description = EXCLUDED.description,
                    config_json = EXCLUDED.config_json,
                    built_in = EXCLUDED.built_in,
                    enabled = EXCLUDED.enabled
                """,
                preset.presetId() == null ? UUID.randomUUID() : preset.presetId(),
                preset.tenantId(),
                preset.code(),
                preset.name(),
                preset.description(),
                JsonSupport.write(preset.config()),
                preset.builtIn(),
                preset.enabled(),
                Timestamp.from(now)
        );
        return findSceneRulePreset(preset.tenantId(), preset.code()).orElse(preset);
    }

    @Override
    public List<LibraryTypePreset> listLibraryTypePresets(String tenantId) {
        if (tenantId == null) {
            return jdbcTemplate.query(
                    "SELECT preset_id, tenant_id, code, name, description, config_json, built_in, enabled FROM kb_library_type_preset WHERE enabled = TRUE",
                    (resultSet, rowNum) -> new LibraryTypePreset(
                            resultSet.getObject("preset_id", UUID.class),
                            resultSet.getString("tenant_id"),
                            resultSet.getString("code"),
                            resultSet.getString("name"),
                            resultSet.getString("description"),
                            JsonSupport.readMap(resultSet.getString("config_json")),
                            resultSet.getBoolean("built_in"),
                            resultSet.getBoolean("enabled")
                    )
            );
        }
        return jdbcTemplate.query(
                "SELECT preset_id, tenant_id, code, name, description, config_json, built_in, enabled FROM kb_library_type_preset WHERE enabled = TRUE AND tenant_id = ?",
                (resultSet, rowNum) -> new LibraryTypePreset(
                        resultSet.getObject("preset_id", UUID.class),
                        resultSet.getString("tenant_id"),
                        resultSet.getString("code"),
                        resultSet.getString("name"),
                        resultSet.getString("description"),
                        JsonSupport.readMap(resultSet.getString("config_json")),
                        resultSet.getBoolean("built_in"),
                        resultSet.getBoolean("enabled")
                ),
                tenantId
        );
    }

    @Override
    public List<SceneRulePreset> listSceneRulePresets(String tenantId) {
        if (tenantId == null) {
            return jdbcTemplate.query(
                    "SELECT preset_id, tenant_id, code, name, description, config_json, built_in, enabled FROM kb_scene_rule_preset WHERE enabled = TRUE",
                    (resultSet, rowNum) -> new SceneRulePreset(
                            resultSet.getObject("preset_id", UUID.class),
                            resultSet.getString("tenant_id"),
                            resultSet.getString("code"),
                            resultSet.getString("name"),
                            resultSet.getString("description"),
                            JsonSupport.readMap(resultSet.getString("config_json")),
                            resultSet.getBoolean("built_in"),
                            resultSet.getBoolean("enabled")
                    )
            );
        }
        return jdbcTemplate.query(
                "SELECT preset_id, tenant_id, code, name, description, config_json, built_in, enabled FROM kb_scene_rule_preset WHERE enabled = TRUE AND tenant_id = ?",
                (resultSet, rowNum) -> new SceneRulePreset(
                        resultSet.getObject("preset_id", UUID.class),
                        resultSet.getString("tenant_id"),
                        resultSet.getString("code"),
                        resultSet.getString("name"),
                        resultSet.getString("description"),
                        JsonSupport.readMap(resultSet.getString("config_json")),
                        resultSet.getBoolean("built_in"),
                        resultSet.getBoolean("enabled")
                ),
                tenantId
        );
    }

    @Override
    public Optional<LibraryTypePreset> findLibraryTypePreset(String tenantId, String code) {
        List<LibraryTypePreset> presets = jdbcTemplate.query(
                "SELECT preset_id, tenant_id, code, name, description, config_json, built_in, enabled FROM kb_library_type_preset WHERE tenant_id = ? AND code = ?",
                (resultSet, rowNum) -> new LibraryTypePreset(
                        resultSet.getObject("preset_id", UUID.class),
                        resultSet.getString("tenant_id"),
                        resultSet.getString("code"),
                        resultSet.getString("name"),
                        resultSet.getString("description"),
                        JsonSupport.readMap(resultSet.getString("config_json")),
                        resultSet.getBoolean("built_in"),
                        resultSet.getBoolean("enabled")
                ),
                tenantId,
                code
        );
        return presets.isEmpty() ? Optional.empty() : Optional.of(presets.getFirst());
    }

    @Override
    public Optional<SceneRulePreset> findSceneRulePreset(String tenantId, String code) {
        List<SceneRulePreset> presets = jdbcTemplate.query(
                "SELECT preset_id, tenant_id, code, name, description, config_json, built_in, enabled FROM kb_scene_rule_preset WHERE tenant_id = ? AND code = ?",
                (resultSet, rowNum) -> new SceneRulePreset(
                        resultSet.getObject("preset_id", UUID.class),
                        resultSet.getString("tenant_id"),
                        resultSet.getString("code"),
                        resultSet.getString("name"),
                        resultSet.getString("description"),
                        JsonSupport.readMap(resultSet.getString("config_json")),
                        resultSet.getBoolean("built_in"),
                        resultSet.getBoolean("enabled")
                ),
                tenantId,
                code
        );
        return presets.isEmpty() ? Optional.empty() : Optional.of(presets.getFirst());
    }
}
