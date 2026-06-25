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
            String layoutProvider
    ) {
    }

    public static Map<String, Object> mergeForLoad(DocumentProfile documentProfile, Map<String, Object> sourceOptions) {
        Map<String, Object> merged = new HashMap<>();
        merged.put("ocrEngine", "tesseract");
        merged.put("ocrLanguage", "auto");
        merged.put("ocrConfidenceThreshold", 0.6d);
        merged.put("ocrDownweightMode", OcrDownweightMode.DOWNWEIGHT.wireValue());
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
        return Map.copyOf(merged);
    }

    public static IngestionParseOptions resolve(Map<String, Object> options) {
        return new IngestionParseOptions(
                readString(options, "ocrEngine", "ocrEngineCode"),
                readString(options, "ocrLanguage", "ocrLang"),
                readDouble(options, "ocrConfidenceThreshold", 0.6d),
                OcrDownweightMode.from(options == null ? null : options.get("ocrDownweightMode")),
                readString(options, "layoutProvider", "layoutAnalysisProvider")
        );
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
