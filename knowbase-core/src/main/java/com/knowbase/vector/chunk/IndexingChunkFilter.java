package com.knowbase.vector.chunk;

import java.util.ArrayList;
import java.util.List;

/** 入库前过滤低价值分块，减少表头块进入向量索引。 */
public final class IndexingChunkFilter {

    private IndexingChunkFilter() {}

    public static List<String> removeHeaderOnlyChunks(List<String> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }
        List<String> kept = new ArrayList<>(chunks.size());
        for (String chunk : chunks) {
            if (!WeeklyReportChunkHeuristics.isHeaderOnlyChunk(chunk)) {
                kept.add(chunk);
            }
        }
        // 若全部被判定为表头，保留原列表，避免文档完全无向量
        return kept.isEmpty() ? List.copyOf(chunks) : kept;
    }
}
