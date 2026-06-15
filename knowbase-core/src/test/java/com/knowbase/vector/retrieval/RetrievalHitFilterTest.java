package com.knowbase.vector.retrieval;

import com.knowbase.vector.dto.SearchHit;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RetrievalHitFilterTest {

    private static final String HEADER = """
            序号\t类别\t工作内容\t计划完成时间\t责任人
            """;

    private static final String ROWS = """
            1\t海图项目\t配合苏研院同事编写用户手册\t45896\t杜鹏飞
            """;

    @Test
    void prefersContentChunksOverHeaders() {
        UUID docId = UUID.randomUUID();
        SearchHit header = new SearchHit(UUID.randomUUID(), docId, "t", 1, 0, HEADER, 0.9);
        SearchHit content = new SearchHit(UUID.randomUUID(), docId, "t", 1, 1, ROWS, 0.5);

        List<SearchHit> filtered = RetrievalHitFilter.preferContentChunks(List.of(header, content), 1);

        assertEquals(1, filtered.size());
        assertEquals(content.chunkId(), filtered.get(0).chunkId());
    }

    @Test
    void fillsWithHeadersWhenNoContentAvailable() {
        UUID docId = UUID.randomUUID();
        SearchHit header = new SearchHit(UUID.randomUUID(), docId, "t", 1, 0, HEADER, 0.9);

        List<SearchHit> filtered = RetrievalHitFilter.preferContentChunks(List.of(header), 1);

        assertEquals(1, filtered.size());
        assertFalse(filtered.isEmpty());
    }
}
