package com.knowbase.domain.model;

import java.util.Map;

public record SceneRulePreset(
        String code,
        String name,
        String description,
        Map<String, Object> config,
        boolean builtIn,
        boolean enabled
) {
}
