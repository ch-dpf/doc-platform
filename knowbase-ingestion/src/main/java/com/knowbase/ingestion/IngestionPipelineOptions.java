package com.knowbase.ingestion;

import java.util.Map;
import java.util.UUID;

public final class IngestionPipelineOptions {

    public static final String TARGET_INDEX_GENERATION_ID = "targetIndexGenerationId";
    public static final String DEFER_DOCUMENT_GENERATION_UPDATE = "deferDocumentGenerationUpdate";
    public static final String REBUILD = "rebuild";
    public static final String AUTO_EVAL_DRAFTS = "autoEvalDrafts";
    public static final String PUBLISH_INDEX_ON_SUCCESS = "publishIndexOnSuccess";

    private IngestionPipelineOptions() {
    }

    public static UUID targetIndexGenerationId(Map<String, Object> options) {
        if (options == null) {
            return null;
        }
        Object value = options.get(TARGET_INDEX_GENERATION_ID);
        if (value == null) {
            return null;
        }
        if (value instanceof UUID uuid) {
            return uuid;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        return UUID.fromString(text);
    }

    public static boolean deferDocumentGenerationUpdate(Map<String, Object> options) {
        return options != null && Boolean.TRUE.equals(options.get(DEFER_DOCUMENT_GENERATION_UPDATE));
    }

    public static boolean rebuild(Map<String, Object> options) {
        return options != null && Boolean.TRUE.equals(options.get(REBUILD));
    }

    public static boolean autoEvalDrafts(Map<String, Object> options) {
        if (options == null || !options.containsKey(AUTO_EVAL_DRAFTS)) {
            return true;
        }
        return Boolean.TRUE.equals(options.get(AUTO_EVAL_DRAFTS));
    }

    public static boolean publishIndexOnSuccess(Map<String, Object> options) {
        if (options == null || !options.containsKey(PUBLISH_INDEX_ON_SUCCESS)) {
            return true;
        }
        return Boolean.TRUE.equals(options.get(PUBLISH_INDEX_ON_SUCCESS));
    }
}
