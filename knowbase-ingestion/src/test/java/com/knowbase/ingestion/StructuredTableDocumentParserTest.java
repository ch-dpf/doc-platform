package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.status.ContentFamily;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructuredTableDocumentParserTest {

    @Test
    void parsesCsvAsTableRowBlocks() {
        ParsedDocument parsed = new StructuredTableDocumentParser().parse(new DocumentSource(
                "memory://sales.csv",
                "sales.csv",
                "text/csv",
                new ByteArrayInputStream("""
                        Region,Q1,Q2
                        APAC,10,12
                        EMEA,8,9
                        """.getBytes(StandardCharsets.UTF_8)),
                Map.of("filename", "sales.csv")
        ));

        assertEquals(ContentFamily.STRUCTURED_TABLE, parsed.contentFamily());
        assertEquals("table-deep", parsed.metadata().get("parser"));
        assertEquals(2, parsed.metadata().get("rowGroupCount"));
        assertTrue(parsed.structureAware());
        assertEquals(2, parsed.blocks().size());
        assertTrue(parsed.blocks().stream().allMatch(block -> "table_row".equals(block.blockType())));
        assertTrue(parsed.blocks().getFirst().content().contains("Region=APAC"));
        assertTrue(parsed.blocks().getFirst().content().contains("Q1=10"));
        assertEquals("1", parsed.blocks().getFirst().metadata().get("rowRange"));
        assertEquals("0:2", parsed.blocks().getFirst().metadata().get("columnRange"));
        assertEquals(List.of("Region", "Q1", "Q2"), parsed.blocks().getFirst().metadata().get("headerPath"));
        assertTrue(parsed.blocks().getFirst().metadata().containsKey("cellCoordinates"));
    }

    @Test
    void tableRowsBecomeSemanticSegments() {
        ParsedDocument parsed = new StructuredTableDocumentParser().parse(new DocumentSource(
                "memory://sales.csv",
                "sales.csv",
                "text/csv",
                new ByteArrayInputStream("""
                        Region,Q1
                        APAC,10
                        EMEA,8
                        """.getBytes(StandardCharsets.UTF_8)),
                Map.of()
        ));
        DocumentProfile tableProfile = new DocumentProfile(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "default_table",
                ContentFamily.STRUCTURED_TABLE,
                "table-deep",
                "table_row_token_window",
                null,
                Map.of(),
                Map.of(),
                true
        );

        List<StructuralSegment> segments = new StructureSegmenter().segment(parsed, tableProfile);

        assertEquals(2, segments.size());
        assertTrue(segments.stream().allMatch(segment -> "table_row".equals(segment.boundaryType())));
    }

    @Test
    void parsesSpreadsheetWithMergedCellMetadata() throws Exception {
        byte[] workbookBytes;
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sales");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Region");
            header.createCell(1).setCellValue("Q1");
            header.createCell(2).setCellValue("Q2");
            Row data = sheet.createRow(1);
            data.createCell(0).setCellValue("APAC");
            data.createCell(1).setCellValue("10");
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, 2));
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            workbook.write(output);
            workbookBytes = output.toByteArray();
        }

        ParsedDocument parsed = new StructuredTableDocumentParser().parse(new DocumentSource(
                "memory://sales.xlsx",
                "sales.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new ByteArrayInputStream(workbookBytes),
                Map.of()
        ));

        StructuralBlock row = parsed.blocks().getFirst();
        assertEquals("Sales", row.metadata().get("sheetName"));
        assertEquals(1, row.metadata().get("rowStart"));
        assertEquals(2, row.metadata().get("columnEnd"));
        assertTrue((Boolean) row.metadata().get("hasMergedCells"));
        assertTrue(row.metadata().containsKey("mergedCells"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cells = (List<Map<String, Object>>) row.metadata().get("cellCoordinates");
        assertTrue(cells.stream().anyMatch(cell -> Boolean.TRUE.equals(cell.get("merged"))
                && "R2C2:R2C3".equals(cell.get("mergedRange"))));
    }
}
