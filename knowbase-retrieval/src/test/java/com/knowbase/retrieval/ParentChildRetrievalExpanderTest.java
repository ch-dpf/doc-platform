package com.knowbase.retrieval;

import com.knowbase.domain.model.DocumentChunk;
import com.knowbase.domain.status.ContentFamily;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParentChildRetrievalExpanderTest {

    @Test
    void enrichesChildCandidateWithParentSummary() {
        UUID libraryId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID indexVersionId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();

        Map<UUID, DocumentChunk> chunks = new HashMap<>();
        chunks.put(parentId, new DocumentChunk(
                parentId,
                documentId,
                libraryId,
                indexVersionId,
                "Document summary: Sales Report\nSheets indexed: 1",
                20,
                "tok",
                "1",
                "bge-m3",
                "document_summary",
                null,
                Map.of("chunkRole", "document_summary")
        ));
        chunks.put(childId, new DocumentChunk(
                childId,
                documentId,
                libraryId,
                indexVersionId,
                "Region=APAC | Revenue=100",
                8,
                "tok",
                "1",
                "bge-m3",
                "table_row_group",
                parentId,
                Map.of("chunkRole", "table_row_group")
        ));

        ParentChildRetrievalExpander expander = new ParentChildRetrievalExpander(
                chunkId -> Optional.ofNullable(chunks.get(chunkId))
        );
        List<RetrievalCandidate> expanded = expander.expand(List.of(new RetrievalCandidate(
                libraryId,
                documentId,
                childId,
                indexVersionId,
                "Region=APAC | Revenue=100",
                0.91d,
                Map.of("contentFamily", ContentFamily.STRUCTURED_TABLE.name())
        )), Map.of("expandParentChunks", true));

        assertEquals(2, expanded.size());
        RetrievalCandidate enrichedChild = expanded.getFirst();
        assertTrue(enrichedChild.content().contains("Document summary: Sales Report"));
        assertTrue(enrichedChild.content().contains("Region=APAC | Revenue=100"));
        assertTrue(expanded.stream().anyMatch(candidate -> candidate.chunkId().equals(parentId)));
    }
}
