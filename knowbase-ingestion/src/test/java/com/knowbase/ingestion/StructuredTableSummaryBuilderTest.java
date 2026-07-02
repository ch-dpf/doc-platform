package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentChunk;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StructuredTableSummaryBuilderTest {

    @Test
    void buildDocumentSummaryListsSheetsAndRowCounts() {
        List<DocumentChunk> rows = List.of(
                row("Region=APAC | Q1=10 | Q2=12", List.of("Region", "Q1", "Q2")),
                row("Region=EMEA | Q1=8 | Q2=9", List.of("Region", "Q1", "Q2"))
        );
        ParsedDocument document = new ParsedDocument(
                "memory://weekly.csv",
                "weekly.csv",
                "",
                com.knowbase.domain.status.ContentFamily.STRUCTURED_TABLE,
                Map.of(),
                List.of()
        );

        String summary = StructuredTableSummaryBuilder.buildDocumentSummaryText(
                document,
                Map.of("Weekly", rows)
        );

        assertTrue(summary.contains("Document summary: weekly.csv"));
        assertTrue(summary.contains("Sheets indexed: 1"));
        assertTrue(summary.contains("Weekly: 2 rows"));
        assertTrue(summary.contains("Region, Q1, Q2"));
        assertTrue(summary.contains("APAC"));
    }

    private static DocumentChunk row(String content, List<String> headers) {
        return new DocumentChunk(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                content,
                12,
                "approx-test",
                "1",
                "bge-m3",
                "table_row",
                null,
                Map.of("headerPath", headers, "chunkRole", "flat", "sheetName", "Weekly")
        );
    }
}
