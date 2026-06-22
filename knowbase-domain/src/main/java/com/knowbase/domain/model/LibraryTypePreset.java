package com.knowbase.domain.model;

import java.util.Map;
import java.util.UUID;

public record LibraryTypePreset(
        UUID presetId,
        String tenantId,
        String code,
        String name,
        String description,
        Map<String, Object> config,
        boolean builtIn,
        boolean enabled
) {
    public LibraryTypePreset(
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
