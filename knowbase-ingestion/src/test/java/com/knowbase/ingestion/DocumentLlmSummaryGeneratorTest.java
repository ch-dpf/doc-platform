package com.knowbase.ingestion;

import com.knowbase.domain.status.ContentFamily;
import com.knowbase.ingestion.summary.LongContentSampler;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentLlmSummaryGeneratorTest {

    @Test
    void samplesLongStructuredDocumentsWithHeadMiddleTail() {
        StringBuilder body = new StringBuilder();
        for (int index = 0; index < 200; index++) {
            body.append("Section ").append(index).append(" discusses topic ").append(index).append(".\n\n");
        }
        ParsedDocument document = new ParsedDocument(
                "memory://long.txt",
                "Long",
                body.toString(),
                ContentFamily.RICH_TEXT,
                Map.of(),
                List.of(StructuralBlock.paragraph(body.toString(), 0))
        );

        String sampled = DocumentLlmSummaryGenerator.sampleDocumentText(document, 600);
        assertTrue(sampled.startsWith("Section 0"));
        assertTrue(sampled.contains(LongContentSampler.OMIT_MARKER));
        assertTrue(sampled.length() <= 600);
    }

    @Test
    void samplesStructuredTableWithRowFactsNotColumnStatistics() {
        ParsedDocument document = new ParsedDocument(
                "memory://report.csv",
                "Q2-Sales-Report.csv",
                "",
                ContentFamily.STRUCTURED_TABLE,
                Map.of(),
                List.of(
                        new StructuralBlock(
                                "table_row",
                                0,
                                "Period=2026-05-06 to 2026-05-09 | Owner=Alice | Project=Alpha | Task=Prepare test materials | Status=Done",
                                0,
                                Map.of("sheetName", "Sheet1", "rowIndex", 1)
                        ),
                        new StructuralBlock(
                                "table_row",
                                0,
                                "Period=2026-05-11 to 2026-05-15 | Owner=Alice | Project=Alpha | Task=Follow-up remediation | Status=Pending",
                                1,
                                Map.of("sheetName", "Sheet1", "rowIndex", 2)
                        )
                )
        );
        String sampled = DocumentLlmSummaryGenerator.sampleDocumentText(document, 4000);
        assertTrue(sampled.contains("Alice"));
        assertTrue(sampled.contains("Prepare test materials"));
        assertTrue(sampled.contains("Row 1:"));
        assertTrue(!sampled.contains("Columns:") && !sampled.contains("distinct"));
    }
}
