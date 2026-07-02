package com.knowbase.model.vision;

import java.util.Locale;
import java.util.Map;

public final class VisionDocumentPrompts {

    /** PaddleOCR-VL vLLM task prompt for general text / form OCR. */
    public static final String PADDLEOCR_VL_OCR_PROMPT = "OCR:";

    /** PaddleOCR-VL vLLM task prompt for table-heavy pages. */
    public static final String PADDLEOCR_VL_TABLE_PROMPT = "Table Recognition:";

    public static final String DEFAULT_PAGE_PROMPT = """
            Extract all text from this document page. Preserve natural reading order.
            Use markdown: # for main headings, ## for subheadings.
            Separate paragraphs with blank lines.
            For tables, output pipe-separated rows, e.g. col1 | col2 | col3.
            Output only the extracted document content with no commentary or preamble.
            """;

    private VisionDocumentPrompts() {
    }

    public static String resolvePagePrompt(Map<String, Object> options) {
        return resolvePagePrompt(options, null);
    }

    public static String resolvePagePrompt(Map<String, Object> options, String modelName) {
        String custom = readCustomPrompt(options);
        if (custom != null) {
            return custom;
        }
        if (isPaddleOcrVlModel(modelName)) {
            if (prefersPaddleOcrVlTablePrompt(options)) {
                return PADDLEOCR_VL_TABLE_PROMPT;
            }
            return PADDLEOCR_VL_OCR_PROMPT;
        }
        return DEFAULT_PAGE_PROMPT;
    }

    public static boolean isPaddleOcrVlModel(String modelName) {
        if (modelName == null || modelName.isBlank()) {
            return false;
        }
        String normalized = modelName.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("paddleocr-vl") || normalized.contains("paddleocr_vl");
    }

    public static boolean prefersImageBeforeText(String modelName) {
        return isPaddleOcrVlModel(modelName);
    }

    private static String readCustomPrompt(Map<String, Object> options) {
        if (options == null) {
            return null;
        }
        Object custom = options.get("visionLanguagePrompt");
        if (custom == null) {
            custom = options.get("vlPrompt");
        }
        if (custom == null || String.valueOf(custom).isBlank()) {
            return null;
        }
        return String.valueOf(custom).trim();
    }

    private static boolean prefersPaddleOcrVlTablePrompt(Map<String, Object> options) {
        if (options == null) {
            return false;
        }
        Object mode = options.get("vlTask");
        if (mode == null) {
            mode = options.get("paddleOcrVlTask");
        }
        if (mode == null) {
            return false;
        }
        String normalized = String.valueOf(mode).trim().toLowerCase(Locale.ROOT);
        return "table".equals(normalized) || "table-recognition".equals(normalized);
    }
}
