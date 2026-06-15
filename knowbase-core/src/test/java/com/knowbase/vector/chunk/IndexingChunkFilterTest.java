package com.knowbase.vector.chunk;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndexingChunkFilterTest {

    private static final String HEADER = """
            序号\t类别\t工作内容\t计划完成时间
            """;

    private static final String ROWS = """
            1\t海图项目\t配合苏研院同事编写用户手册
            """;

    @Test
    void removesHeaderOnlyChunks() {
        List<String> filtered = IndexingChunkFilter.removeHeaderOnlyChunks(List.of(HEADER, ROWS));

        assertEquals(1, filtered.size());
        assertTrue(filtered.get(0).contains("配合苏研院"));
    }

    @Test
    void keepsOriginalWhenAllHeaderOnly() {
        List<String> input = List.of(HEADER, HEADER);
        List<String> filtered = IndexingChunkFilter.removeHeaderOnlyChunks(input);

        assertEquals(input, filtered);
    }
}
