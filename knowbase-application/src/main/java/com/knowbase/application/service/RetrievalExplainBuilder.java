package com.knowbase.application.service;

import com.knowbase.retrieval.RetrievalCandidate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class RetrievalExplainBuilder {

    private RetrievalExplainBuilder() {
    }

    static List<Map<String, Object>> buildRankedExplain(List<RetrievalCandidate> candidates, int limit) {
        List<Map<String, Object>> explain = new ArrayList<>();
        int max = Math.min(Math.max(limit, 1), candidates.size());
        for (int index = 0; index < max; index++) {
            RetrievalCandidate candidate = candidates.get(index);
            Map<String, Object> item = new HashMap<>();
            item.put("rank", index + 1);
            item.put("documentId", candidate.documentId());
            item.put("chunkId", candidate.chunkId());
            item.put("score", candidate.score());
            item.put("contentPreview", preview(candidate.content()));
            if (candidate.metadata() != null) {
                copyMeta(item, candidate.metadata(), "vectorScore");
                copyMeta(item, candidate.metadata(), "keywordScore");
                copyMeta(item, candidate.metadata(), "vectorRank");
                copyMeta(item, candidate.metadata(), "keywordRank");
                copyMeta(item, candidate.metadata(), "pageNumber");
                copyMeta(item, candidate.metadata(), "bbox");
                copyMeta(item, candidate.metadata(), "contentFamily");
                copyMeta(item, candidate.metadata(), "sourceUri");
                copyMeta(item, candidate.metadata(), "title");
                copyMeta(item, candidate.metadata(), "retrievalBackend");
            }
            explain.add(Map.copyOf(item));
        }
        return List.copyOf(explain);
    }

    private static void copyMeta(Map<String, Object> target, Map<String, Object> metadata, String key) {
        if (metadata.get(key) != null) {
            target.put(key, metadata.get(key));
        }
    }

    private static String preview(String content) {
        if (content == null) {
            return "";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 160 ? normalized : normalized.substring(0, 157) + "...";
    }
}
