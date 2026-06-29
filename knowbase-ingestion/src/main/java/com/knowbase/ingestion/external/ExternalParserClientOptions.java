package com.knowbase.ingestion.external;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;

/**
 * Per-document external parser HTTP options from {@link com.knowbase.ingestion.DocumentSource#metadata()}.
 */
public record ExternalParserClientOptions(
        Duration timeout,
        int maxAttempts,
        String authBearerToken,
        boolean fallbackEnabled,
        boolean failOnExternalError,
        boolean useJsonRequest
) {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);
    private static final int DEFAULT_MAX_ATTEMPTS = 2;

    public static ExternalParserClientOptions from(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return defaults();
        }
        return new ExternalParserClientOptions(
                readTimeout(metadata),
                readMaxAttempts(metadata),
                readAuthToken(metadata),
                ExternalParserFallbackResolver.isFallbackEnabled(metadata),
                readBoolean(metadata, "failOnExternalError", false),
                readBoolean(metadata, "externalParserUseJsonRequest", false)
                        || readBoolean(metadata, "externalParserJsonRequest", false)
        );
    }

    public static ExternalParserClientOptions defaults() {
        return new ExternalParserClientOptions(
                DEFAULT_TIMEOUT,
                DEFAULT_MAX_ATTEMPTS,
                null,
                true,
                false,
                false
        );
    }

    private static Duration readTimeout(Map<String, Object> metadata) {
        Object raw = metadata.get("externalParserTimeoutSeconds");
        if (raw instanceof Number number) {
            return Duration.ofSeconds(Math.max(1, number.longValue()));
        }
        if (raw != null) {
            try {
                return Duration.ofSeconds(Math.max(1, Long.parseLong(String.valueOf(raw).trim())));
            } catch (NumberFormatException ignored) {
                return DEFAULT_TIMEOUT;
            }
        }
        Object millis = metadata.get("externalParserTimeoutMillis");
        if (millis instanceof Number number) {
            return Duration.ofMillis(Math.max(1, number.longValue()));
        }
        return DEFAULT_TIMEOUT;
    }

    private static int readMaxAttempts(Map<String, Object> metadata) {
        Object raw = metadata.get("externalParserMaxAttempts");
        if (raw instanceof Number number) {
            return Math.max(1, Math.min(5, number.intValue()));
        }
        if (raw != null) {
            try {
                return Math.max(1, Math.min(5, Integer.parseInt(String.valueOf(raw).trim())));
            } catch (NumberFormatException ignored) {
                return DEFAULT_MAX_ATTEMPTS;
            }
        }
        return DEFAULT_MAX_ATTEMPTS;
    }

    private static String readAuthToken(Map<String, Object> metadata) {
        for (String key : new String[] {
                "externalParserBearerToken",
                "externalParserAuthToken",
                "externalParserApiKey"
        }) {
            Object value = metadata.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                return String.valueOf(value).trim();
            }
        }
        return null;
    }

    private static boolean readBoolean(Map<String, Object> metadata, String key, boolean defaultValue) {
        Object raw = metadata.get(key);
        if (raw == null) {
            return defaultValue;
        }
        if (raw instanceof Boolean bool) {
            return bool;
        }
        return "true".equalsIgnoreCase(String.valueOf(raw).trim());
    }
}
