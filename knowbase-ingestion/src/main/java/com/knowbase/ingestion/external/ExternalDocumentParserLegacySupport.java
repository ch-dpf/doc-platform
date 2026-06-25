package com.knowbase.ingestion.external;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ExternalDocumentParserLegacySupport {

    private static final Pattern JSON_TEXT = Pattern.compile("\"(?:text|markdown|content)\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");

    private ExternalDocumentParserLegacySupport() {
    }

    public static String extractText(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "";
        }
        Matcher matcher = JSON_TEXT.matcher(responseBody);
        if (matcher.find()) {
            return unescapeJson(matcher.group(1));
        }
        return responseBody;
    }

    private static String unescapeJson(String value) {
        return value
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
