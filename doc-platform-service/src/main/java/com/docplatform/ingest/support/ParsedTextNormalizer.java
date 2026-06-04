package com.docplatform.ingest.support;

import com.docplatform.ingest.config.TextNormalizationProperties;
import com.docplatform.library.config.TextNormalizationSettings;
import org.springframework.stereotype.Component;

@Component
public class ParsedTextNormalizer {

    private final TextNormalizationSettings globalSettings;

    public ParsedTextNormalizer(TextNormalizationProperties properties) {
        this.globalSettings = TextNormalizationSettings.fromGlobal(properties);
    }

    public String normalize(String raw) {
        return normalize(raw, globalSettings);
    }

    public String normalize(String raw, TextNormalizationSettings settings) {
        return TextNormalizationEngine.normalize(raw, settings);
    }
}
