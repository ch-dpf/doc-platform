package com.knowbase.vector.retrieval;

import com.knowbase.vector.chunk.WeeklyReportChunkHeuristics;
import com.knowbase.vector.dto.SearchHit;

import java.util.ArrayList;
import java.util.List;

/** RAG 检索结果后处理：优先保留含正文的分块。 */
public final class RetrievalHitFilter {

    private RetrievalHitFilter() {}

    /**
     * 将仅含表头的周报块降到队尾；优先返回有数据行的片段。
     * 过滤后不足 topK 时用表头块补足，避免零结果。
     */
    public static List<SearchHit> preferContentChunks(List<SearchHit> hits, int topK) {
        if (hits == null || hits.isEmpty() || topK <= 0) {
            return List.of();
        }
        List<SearchHit> content = new ArrayList<>();
        List<SearchHit> headers = new ArrayList<>();
        for (SearchHit hit : hits) {
            if (WeeklyReportChunkHeuristics.isHeaderOnlyChunk(hit.content())) {
                headers.add(hit);
            } else {
                content.add(hit);
            }
        }
        List<SearchHit> ordered = new ArrayList<>(content);
        for (SearchHit header : headers) {
            if (ordered.size() >= topK) {
                break;
            }
            ordered.add(header);
        }
        if (ordered.size() <= topK) {
            return List.copyOf(ordered);
        }
        return List.copyOf(ordered.subList(0, topK));
    }
}
