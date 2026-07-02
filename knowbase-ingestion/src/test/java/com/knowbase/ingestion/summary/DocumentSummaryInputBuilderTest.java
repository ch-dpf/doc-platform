package com.knowbase.ingestion.summary;

import com.knowbase.domain.model.DocumentChunk;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentSummaryInputBuilderTest {

    @Test
    void prefersChunkBusinessTextOverSummaryChunks() {
        UUID libraryId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        DocumentChunk summary = new DocumentChunk(
                UUID.randomUUID(),
                documentId,
                libraryId,
                UUID.randomUUID(),
                "Document summary: Report\nColumns: dept distinct=3",
                10,
                "tok",
                "1",
                "model",
                "document_summary",
                null,
                Map.of("chunkRole", "document_summary")
        );
        DocumentChunk row = new DocumentChunk(
                UUID.randomUUID(),
                documentId,
                libraryId,
                summary.indexVersionId(),
                "Period=2026-05-06 | Owner=Alice | Task=Prepare materials | Status=Done",
                8,
                "tok",
                "1",
                "model",
                "table_row",
                null,
                Map.of("chunkRole", "table_row_group", "rowStart", 1, "flatOrdinal", 1)
        );
        String built = DocumentSummaryInputBuilder.buildFromChunks(List.of(summary, row), 4000);
        assertTrue(built.contains("Alice"));
        assertTrue(built.contains("Prepare materials"));
        assertTrue(!built.contains("Columns:"));
        assertTrue(!built.contains("distinct"));
    }
}
