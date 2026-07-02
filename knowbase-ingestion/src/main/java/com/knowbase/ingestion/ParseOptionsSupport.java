package com.knowbase.ingestion;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 解析模式与 parserCode 映射：standard / layout / ocr。
 */
public final class ParseOptionsSupport {

    public static final String PARSE_MODE_STANDARD = "standard";
    public static final String PARSE_MODE_LAYOUT = "layout";
    public static final String PARSE_MODE_OCR = "ocr";

    private ParseOptionsSupport() {
    }

    public static Map<String, Object> applyParseMode(Map<String, Object> options, String sourceUri) {
        if (options == null || options.isEmpty()) {
            return options == null ? Map.of() : options;
        }
        HashMap<String, Object> merged = new HashMap<>(options);
        String parseMode = resolveParseMode(options);
        if (parseMode == null) {
            return Map.copyOf(merged);
        }
        merged.put("parseMode", parseMode);
        String parserCode = resolveParserCode(parseMode, sourceUri, stringOption(options, "parserCode"));
        if (parserCode != null) {
            merged.put("parserCode", parserCode);
        }
        if (PARSE_MODE_OCR.equals(parseMode)) {
            merged.putIfAbsent("enableOcr", true);
            merged.putIfAbsent("ocrApplied", true);
        }
        if (PARSE_MODE_LAYOUT.equals(parseMode)) {
            merged.putIfAbsent("layoutParsing", true);
        }
        return Map.copyOf(merged);
    }

    public static String resolveParseMode(Map<String, Object> options) {
        if (options == null) {
            return null;
        }
        String explicit = firstNonBlank(
                stringOption(options, "parseMode"),
                stringOption(options, "parseProfile")
        );
        if (explicit != null) {
            return normalizeParseMode(explicit);
        }
        if (isTruthy(options.get("enableOcr")) || isTruthy(options.get("ocrEnabled"))) {
            return PARSE_MODE_OCR;
        }
        if (isTruthy(options.get("enableLayoutParsing")) || isTruthy(options.get("layoutParsingEnabled"))) {
            return PARSE_MODE_LAYOUT;
        }
        return null;
    }

    public static String resolveParserCode(String parseMode, String sourceUri, String fallbackParserCode) {
        if (parseMode == null) {
            return fallbackParserCode;
        }
        String lowerUri = sourceUri == null ? "" : sourceUri.toLowerCase(Locale.ROOT);
        return switch (parseMode) {
            case PARSE_MODE_LAYOUT -> {
                if (lowerUri.endsWith(".pdf")) {
                    yield PdfLayoutParser.PARSER_CODE;
                }
                yield fallbackParserCode;
            }
            case PARSE_MODE_OCR -> {
                if (lowerUri.endsWith(".pdf")
                        || lowerUri.endsWith(".png")
                        || lowerUri.endsWith(".jpg")
                        || lowerUri.endsWith(".jpeg")
                        || lowerUri.endsWith(".bmp")
                        || lowerUri.endsWith(".webp")
                        || lowerUri.endsWith(".tif")
                        || lowerUri.endsWith(".tiff")) {
                    yield OcrLayoutDocumentParser.PARSER_CODE;
                }
                yield fallbackParserCode;
            }
            default -> fallbackParserCode;
        };
    }

    public static boolean isLayoutMode(Map<String, Object> options) {
        return PARSE_MODE_LAYOUT.equals(resolveParseMode(options));
    }

    public static boolean isOcrMode(Map<String, Object> options) {
        return PARSE_MODE_OCR.equals(resolveParseMode(options));
    }

    private static String normalizeParseMode(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "standard", "structure", "auto" -> PARSE_MODE_STANDARD;
            case "layout", "layout-aware", "pdf-layout" -> PARSE_MODE_LAYOUT;
            case "ocr", "ocr-layout", "scan", "scanned" -> PARSE_MODE_OCR;
            default -> normalized;
        };
    }

    private static boolean isTruthy(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value == null) {
            return false;
        }
        String text = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
        return "true".equals(text) || "1".equals(text) || "yes".equals(text) || "on".equals(text);
    }

    private static String stringOption(Map<String, Object> options, String key) {
        Object value = options.get(key);
        return value == null || String.valueOf(value).isBlank() ? null : String.valueOf(value);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
