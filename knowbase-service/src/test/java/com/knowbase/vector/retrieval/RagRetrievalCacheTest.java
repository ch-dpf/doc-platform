package com.knowbase.vector.retrieval;

import com.knowbase.vector.config.RetrievalProperties;
import com.knowbase.vector.dto.SearchHit;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagRetrievalCacheTest {

    @Test
    void cachesAndReusesHits() {
        RetrievalProperties props = new RetrievalProperties();
        props.setCacheEnabled(true);
        props.setCacheTtlSeconds(300);
        RagRetrievalCache cache = new RagRetrievalCache(props);
        UUID libraryId = UUID.randomUUID();
        SearchHit hit = new SearchHit(UUID.randomUUID(), UUID.randomUUID(), "demo", 1, 0, "content", 0.8);

        cache.put(libraryId, "demo", "周报汇总", "周报", 10, null, null, List.of(hit));
        var cached = cache.get(libraryId, "demo", "周报汇总", "周报", 10, null, null);

        assertNotNull(cached);
        assertTrue(cached.cacheHit());
        assertEquals(1, cached.hits().size());
    }

    @Test
    void disabledCacheReturnsNull() {
        RetrievalProperties props = new RetrievalProperties();
        props.setCacheEnabled(false);
        RagRetrievalCache cache = new RagRetrievalCache(props);
        assertNull(cache.get(UUID.randomUUID(), "demo", "q", "q", 5, null, null));
    }

    @Test
    void differentQuestionMissesCache() {
        RetrievalProperties props = new RetrievalProperties();
        props.setCacheEnabled(true);
        RagRetrievalCache cache = new RagRetrievalCache(props);
        UUID libraryId = UUID.randomUUID();
        SearchHit hit = new SearchHit(UUID.randomUUID(), UUID.randomUUID(), "demo", 1, 0, "c", 0.5);
        cache.put(libraryId, "demo", "问题A", "问题A", 5, null, null, List.of(hit));

        assertNotNull(cache.get(libraryId, "demo", "问题A", "问题A", 5, null, null));
        assertNull(cache.get(libraryId, "demo", "问题B", "问题B", 5, null, null));
    }
}
