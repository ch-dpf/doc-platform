package com.knowbase.application.mapper;

import com.knowbase.domain.model.DocumentChunk;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentChunkPresentationTest {

    private static final UUID DOC_ID = UUID.randomUUID();
    private static final UUID LIB_ID = UUID.randomUUID();
    private static final UUID INDEX_ID = UUID.randomUUID();

    @Test
    void detectsSummaryByBoundaryType() {
        DocumentChunk summary = chunk("document_summary", Map.of());
        assertTrue(DocumentChunkPresentation.isSummaryChunk(summary));
    }

    @Test
    void detectsSummaryByMetadataRole() {
        DocumentChunk summary = chunk("table_row", Map.of("chunkRole", "document_summary"));
        assertTrue(DocumentChunkPresentation.isSummaryChunk(summary));
    }

    @Test
    void excludesSummaryChunksAndPagesRetrievalBlocks() {
        DocumentChunk retrieval = chunk("table_row", Map.of("chunkRole", "table_row"));
        DocumentChunk summary = chunk("document_summary", Map.of("chunkRole", "document_summary"));
        List<DocumentChunk> visible = DocumentChunkPresentation.excludeSummaryChunks(List.of(summary, retrieval, retrieval));

        assertEquals(2, visible.size());
        assertFalse(visible.stream().anyMatch(DocumentChunkPresentation::isSummaryChunk));

        List<DocumentChunk> page = DocumentChunkPresentation.page(visible, 1, 1);
        assertEquals(1, page.size());
        assertEquals(retrieval.chunkId(), page.getFirst().chunkId());
    }

    private static DocumentChunk chunk(String boundaryType, Map<String, Object> metadata) {
        return new DocumentChunk(
                UUID.randomUUID(),
                DOC_ID,
                LIB_ID,
                INDEX_ID,
                "content",
                10,
                "tokenizer",
                "v1",
                "embedding",
                boundaryType,
                null,
                metadata
        );
    }
}
