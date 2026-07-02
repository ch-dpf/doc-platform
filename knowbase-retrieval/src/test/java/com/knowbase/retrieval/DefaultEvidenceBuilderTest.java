package com.knowbase.retrieval;

import com.knowbase.domain.model.Citation;
import com.knowbase.domain.model.EvidencePack;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DefaultEvidenceBuilderTest {

    @Test
    void copiesCellCoordinatesAndPrimaryCellRef() {
        UUID libraryId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();
        UUID indexVersionId = UUID.randomUUID();
        Map<String, Object> chunkMetadata = Map.of(
                "title", "季度报表",
                "sourceUri", "file://report.xlsx",
                "sheetName", "Metrics",
                "rowIndex", 1,
                "primaryCellRef", "A2",
                "cellRefs", List.of("A2", "B2"),
                "cellCoordinates", List.of(
                        Map.of("rowIndex", 1, "columnIndex", 0, "cellRef", "A2", "value", "张三"),
                        Map.of("rowIndex", 1, "columnIndex", 1, "cellRef", "B2", "value", "研发")
                ),
                "evidenceAssetHint", Map.of(
                        "kind", "sheet_row",
                        "sheetName", "Metrics",
                        "primaryCellRef", "A2"
                )
        );

        EvidencePack pack = new DefaultEvidenceBuilder().build(List.of(new RetrievalCandidate(
                libraryId,
                documentId,
                chunkId,
                indexVersionId,
                "张三 | 研发",
                0.92d,
                chunkMetadata
        )));

        assertEquals(1, pack.citations().size());
        Citation citation = pack.citations().getFirst();
        assertEquals("季度报表", citation.sourceTitle());
        assertEquals("Metrics", citation.metadata().get("sheetName"));
        assertEquals("A2", citation.metadata().get("primaryCellRef"));
        assertEquals(List.of("A2", "B2"), citation.metadata().get("cellRefs"));
        assertNotNull(citation.metadata().get("cellCoordinates"));
        assertNotNull(citation.metadata().get("evidenceAssetHint"));
        assertEquals(chunkId.toString(), citation.metadata().get("chunkId"));
    }

    @Test
    void copiesPdfCitationLocationFieldsFromChunkMetadata() {
        UUID libraryId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();
        UUID indexVersionId = UUID.randomUUID();
        Map<String, Object> chunkMetadata = Map.of(
                "title", "季度报表",
                "sourceUri", "file://report.pdf",
                "pageNumber", 3,
                "bbox", List.of(72, 100, 400, 120),
                "tableRegionLabel", "table-0",
                "headerPath", List.of("姓名", "部门"),
                "rowRole", "DATA",
                "evidenceAssetHint", Map.of("kind", "pdf_page", "pageNumber", 3)
        );

        EvidencePack pack = new DefaultEvidenceBuilder().build(List.of(new RetrievalCandidate(
                libraryId,
                documentId,
                chunkId,
                indexVersionId,
                "张三 | 研发",
                0.92d,
                chunkMetadata
        )));

        Citation citation = pack.citations().getFirst();
        assertEquals(3, citation.metadata().get("pageNumber"));
        assertEquals("table-0", citation.metadata().get("tableRegionLabel"));
    }
}
