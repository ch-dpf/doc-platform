package com.knowbase.domain.model;

import java.util.Map;
import java.util.UUID;

public record SceneRulePreset(
        UUID presetId,
        String tenantId,
        String code,
        String name,
        String description,
        Map<String, Object> config,
        boolean builtIn,
        boolean enabled
) {
    public SceneRulePreset(
            String code,
            String name,
            String description,
            Map<String, Object> config,
            boolean builtIn,
            boolean enabled
    ) {
        this(null, null, code, name, description, config, builtIn, enabled);
    }
}
