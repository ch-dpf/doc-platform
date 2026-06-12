package com.knowbase.pipeline.chunk;

import com.knowbase.pipeline.content.ContentFamily;
import com.knowbase.pipeline.content.ContentSignals;
import com.knowbase.vector.chunk.ChunkingStrategy;
import com.knowbase.vector.config.ChunkingProperties;

/**
 * 判定是否启用多粒度索引：heading-level 父段 + 子块检索。
 */
public final class HierarchicalChunkingPolicy {

    private static final int MIN_PARENT_CHARS = 600;

    private HierarchicalChunkingPolicy() {
    }

    public static boolean shouldApply(
            ContentFamily family, ChunkingProperties chunking, ContentSignals signals, String text) {
        if (chunking == null
                || !chunking.isHierarchicalChunkingEnabled()
                || chunking.getStrategy() != ChunkingStrategy.HEADING_LEVEL) {
            return false;
        }
        if (signals != null && signals.isShortDocument()) {
            return false;
        }
        if (family != ContentFamily.DOCUMENT && family != ContentFamily.PLAIN) {
            return false;
        }
        return text != null && text.length() >= MIN_PARENT_CHARS;
    }
}
