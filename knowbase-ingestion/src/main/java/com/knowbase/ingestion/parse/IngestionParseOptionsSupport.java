package com.knowbase.ingestion.parse;

import com.knowbase.domain.model.DocumentProfile;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Merges application defaults, {@link DocumentProfile#options()}, and per-document metadata
 * for OCR / layout parsing.
 */
public final class IngestionParseOptionsSupport {

    private IngestionParseOptionsSupport() {
    }

    public record IngestionParseOptions(
            String ocrEngine,
            String ocrLanguage,
            double ocrConfidenceThreshold,
            OcrDownweightMode ocrDownweightMode,
            String layoutProvider,
            String readingOrderEndpoint,
            boolean evidenceArtifactsEnabled
    ) {
    }

    public static Map<String, Object> mergeForLoad(DocumentProfile documentProfile, Map<String, Object> sourceOptions) {
        return mergeForLoad(documentProfile, sourceOptions, Map.of());
    }

    public static Map<String, Object> mergeForLoad(
            DocumentProfile documentProfile,
            Map<String, Object> sourceOptions,
            Map<String, Object> applicationDefaults
    ) {
        Map<String, Object> merged = new HashMap<>();
        if (applicationDefaults != null) {
            merged.putAll(applicationDefaults);
        }
        merged.putIfAbsent("ocrEngine", "tesseract");
        merged.putIfAbsent("ocrLanguage", "auto");
        merged.putIfAbsent("ocrConfidenceThreshold", 0.6d);
        merged.putIfAbsent("ocrDownweightMode", OcrDownweightMode.DOWNWEIGHT.wireValue());
        if (documentProfile != null && documentProfile.options() != null) {
            merged.putAll(documentProfile.options());
        }
        if (sourceOptions != null) {
            merged.putAll(sourceOptions);
        }
        IngestionParseOptions resolved = resolve(merged);
        if (resolved.ocrEngine() != null) {
            merged.putIfAbsent("ocrEngine", resolved.ocrEngine());
        }
        if (resolved.ocrLanguage() != null) {
            merged.putIfAbsent("ocrLanguage", resolved.ocrLanguage());
        }
        merged.putIfAbsent("ocrConfidenceThreshold", resolved.ocrConfidenceThreshold());
        merged.putIfAbsent("ocrDownweightMode", resolved.ocrDownweightMode().wireValue());
        if (resolved.layoutProvider() != null) {
            merged.putIfAbsent("layoutProvider", resolved.layoutProvider());
        }
        if (resolved.readingOrderEndpoint() != null) {
            merged.putIfAbsent("readingOrderEndpoint", resolved.readingOrderEndpoint());
        }
        merged.putIfAbsent("evidenceArtifactsEnabled", resolved.evidenceArtifactsEnabled());
        return Map.copyOf(merged);
    }

    public static Map<String, Object> applicationDefaults(
            String ocrEngine,
            String ocrLanguage,
            double ocrConfidenceThreshold,
            String ocrDownweightMode,
            String layoutProvider,
            String readingOrderEndpoint,
            boolean evidenceArtifactsEnabled
    ) {
        Map<String, Object> defaults = new HashMap<>();
        if (ocrEngine != null) {
            defaults.put("ocrEngine", ocrEngine);
        }
        if (ocrLanguage != null) {
            defaults.put("ocrLanguage", ocrLanguage);
        }
        defaults.put("ocrConfidenceThreshold", ocrConfidenceThreshold);
        if (ocrDownweightMode != null) {
            defaults.put("ocrDownweightMode", ocrDownweightMode);
        }
        if (layoutProvider != null && !layoutProvider.isBlank()) {
            defaults.put("layoutProvider", layoutProvider);
        }
        if (readingOrderEndpoint != null && !readingOrderEndpoint.isBlank()) {
            defaults.put("readingOrderEndpoint", readingOrderEndpoint);
        }
        defaults.put("evidenceArtifactsEnabled", evidenceArtifactsEnabled);
        return Map.copyOf(defaults);
    }

    public static IngestionParseOptions resolve(Map<String, Object> options) {
        return new IngestionParseOptions(
                readString(options, "ocrEngine", "ocrEngineCode"),
                readString(options, "ocrLanguage", "ocrLang"),
                readDouble(options, "ocrConfidenceThreshold", 0.6d),
                OcrDownweightMode.from(options == null ? null : options.get("ocrDownweightMode")),
                readString(options, "layoutProvider", "layoutAnalysisProvider"),
                readString(options, "readingOrderEndpoint", "readingOrderModelEndpoint"),
                readBoolean(options, "evidenceArtifactsEnabled", false)
        );
    }

    private static boolean readBoolean(Map<String, Object> options, String key, boolean defaultValue) {
        if (options == null || options.get(key) == null) {
            return defaultValue;
        }
        Object value = options.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        return !"false".equalsIgnoreCase(String.valueOf(value));
    }

    private static String readString(Map<String, Object> options, String... keys) {
        if (options == null) {
            return null;
        }
        for (String key : keys) {
            Object value = options.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                return String.valueOf(value).trim();
            }
        }
        return null;
    }

    private static double readDouble(Map<String, Object> options, String key, double defaultValue) {
        if (options == null) {
            return defaultValue;
        }
        Object value = options.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value != null) {
            try {
                return Double.parseDouble(String.valueOf(value).trim());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}
