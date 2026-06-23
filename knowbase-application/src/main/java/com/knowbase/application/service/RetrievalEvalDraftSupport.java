package com.knowbase.application.service;

import com.knowbase.domain.model.RetrievalEvalSample;

public final class RetrievalEvalDraftSupport {

    public static final String NOTE_PREFIX = "[auto-draft]";
    public static final int MAX_DRAFTS_PER_GENERATION = 20;
    public static final int DEFAULT_ENABLED_DRAFTS = 5;

    private RetrievalEvalDraftSupport() {
    }

    public static boolean isAutoDraft(RetrievalEvalSample sample) {
        return sample.notes() != null && sample.notes().startsWith(NOTE_PREFIX);
    }

    public static boolean isEnabledByDefault(int indexInBatch) {
        return indexInBatch >= 0 && indexInBatch < DEFAULT_ENABLED_DRAFTS;
    }

    public static String formatNotes(String detail) {
        if (detail == null || detail.isBlank()) {
            return NOTE_PREFIX + " 根据文档内容自动生成；每批前 " + DEFAULT_ENABLED_DRAFTS + " 条默认启用";
        }
        return NOTE_PREFIX + " " + detail.trim();
    }
}
