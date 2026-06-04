package com.docplatform.vector.chunk;

/**
 * 分块前的轻量规范化（兼容历史 parsed.txt 未走 ingest 规范化的情况）。
 */
public final class ChunkTextPreprocessor {

    private ChunkTextPreprocessor() {
    }

    public static String prepare(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n').strip();
        return normalized.replaceAll("\n{3,}", "\n\n");
    }
}
