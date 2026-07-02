package com.knowbase.ingestion.summary;

import com.knowbase.domain.model.DocumentProfile;

import java.util.Map;

/**
 * Effective document-summary settings merged from global defaults and document profile options.
 */
public record DocumentSummarySettings(
        String promptId,
        String language,
        int maxInputChars,
        int maxOutputChars,
        int minInputChars,
        double temperature,
        int maxCompletionTokens
) {

    public static DocumentSummarySettings defaults() {
        return new DocumentSummarySettings(
                "default_summary",
                "the same language as the source content",
                16_384,
                500,
                100,
                0.3,
                2048
        );
    }

    public static DocumentSummarySettings merge(DocumentSummarySettings global, DocumentProfile profile) {
        DocumentSummarySettings base = global == null ? defaults() : global;
        if (profile == null || profile.options() == null) {
            return base;
        }
        Map<String, Object> options = profile.options();
        return new DocumentSummarySettings(
                readString(options, "llmSummaryPromptId", base.promptId()),
                readString(options, "llmSummaryLanguage", base.language()),
                readInt(options, "llmSummaryMaxInputChars", base.maxInputChars()),
                readInt(options, "llmSummaryMaxChars", base.maxOutputChars()),
                readInt(options, "llmSummaryMinInputChars", base.minInputChars()),
                readDouble(options, "llmSummaryTemperature", base.temperature()),
                readInt(options, "llmSummaryMaxCompletionTokens", base.maxCompletionTokens())
        );
    }

    private static String readString(Map<String, Object> options, String key, String defaultValue) {
        Object value = options.get(key);
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? defaultValue : text;
    }

    private static int readInt(Map<String, Object> options, String key, int defaultValue) {
        Object value = options.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value).trim());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private static double readDouble(Map<String, Object> options, String key, double defaultValue) {
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
