package com.knowbase.pipeline.content;

import com.knowbase.vector.config.ChunkingProperties;
import org.springframework.stereotype.Component;

/**
 * 族群级 chunkSize 上下界：在库级 baseline 之上收窄粒度分布，避免同库向量空间漂移过大。
 */
@Component
public class ContentFamilyChunkBounds {

    public void apply(ContentFamily family, ChunkingProperties chunking) {
        if (family == null || chunking == null) {
            return;
        }
        switch (family) {
            case TABULAR -> clamp(chunking, 280, 700);
            case DOCUMENT -> clamp(chunking, 400, 1200);
            case PLAIN -> clamp(chunking, 350, 1000);
            case IMAGE -> clamp(chunking, 400, 900);
            case UNKNOWN -> { /* 仅库级上下界 */ }
        }
    }

    private static void clamp(ChunkingProperties chunking, int minSize, int maxSize) {
        int size = chunking.getChunkSize();
        if (size < minSize) {
            chunking.setChunkSize(minSize);
        } else if (size > maxSize) {
            chunking.setChunkSize(maxSize);
        }
        if (chunking.getMinChunkSize() > minSize / 2) {
            chunking.setMinChunkSize(Math.max(40, minSize / 2));
        }
        if (chunking.getMaxChunkSize() > maxSize) {
            chunking.setMaxChunkSize(maxSize);
        }
        int overlap = chunking.getOverlap();
        int cappedOverlap = Math.min(overlap, Math.max(0, chunking.getChunkSize() / 3));
        chunking.setOverlap(cappedOverlap);
    }
}
