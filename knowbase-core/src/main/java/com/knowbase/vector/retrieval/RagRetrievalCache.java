package com.knowbase.vector.retrieval;

import com.knowbase.platform.JsonSupport;
import com.knowbase.vector.config.RetrievalProperties;
import com.knowbase.vector.dto.SearchHit;
import com.knowbase.vector.dto.SearchRequest;
import com.knowbase.vector.rag.RagTemporalConstants;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 同库同问短时缓存 RAG 检索结果，降低 Top-K 抖动对连续提问的影响。 */
@Component
public class RagRetrievalCache {

    private final RetrievalProperties properties;
    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();

    public RagRetrievalCache(RetrievalProperties properties) {
        this.properties = properties;
    }

    public record CachedHits(List<SearchHit> hits, boolean cacheHit) {}

    public CachedHits get(
            UUID libraryId,
            String tenantId,
            String searchQuery,
            String keywordQuery,
            int topK,
            Double minScore,
            SearchRequest.SearchFilter filter) {
        if (!properties.isCacheEnabled()) {
            return null;
        }
        String key = buildKey(libraryId, tenantId, searchQuery, keywordQuery, topK, minScore, filter);
        Entry entry = store.get(key);
        if (entry == null || entry.expiresAt.isBefore(Instant.now())) {
            if (entry != null) {
                store.remove(key, entry);
            }
            return null;
        }
        return new CachedHits(List.copyOf(entry.hits), true);
    }

    public void put(
            UUID libraryId,
            String tenantId,
            String searchQuery,
            String keywordQuery,
            int topK,
            Double minScore,
            SearchRequest.SearchFilter filter,
            List<SearchHit> hits) {
        if (!properties.isCacheEnabled() || hits == null) {
            return;
        }
        int ttl = Math.max(30, properties.getCacheTtlSeconds());
        String key = buildKey(libraryId, tenantId, searchQuery, keywordQuery, topK, minScore, filter);
        store.put(key, new Entry(List.copyOf(hits), Instant.now().plusSeconds(ttl)));
    }

    public void clear() {
        store.clear();
    }

    static String buildKey(
            UUID libraryId,
            String tenantId,
            String searchQuery,
            String keywordQuery,
            int topK,
            Double minScore,
            SearchRequest.SearchFilter filter) {
        String filterJson = filter == null ? "" : JsonSupport.toJson(filter);
        return RagTemporalConstants.PARSER_VERSION
                + "|" + libraryId
                + "|" + tenantId.trim()
                + "|" + normalize(searchQuery)
                + "|" + normalize(keywordQuery)
                + "|" + topK
                + "|" + (minScore == null ? "null" : minScore)
                + "|" + filterJson;
    }

    private static String normalize(String text) {
        return text == null ? "" : text.strip();
    }

    private record Entry(List<SearchHit> hits, Instant expiresAt) {}
}
