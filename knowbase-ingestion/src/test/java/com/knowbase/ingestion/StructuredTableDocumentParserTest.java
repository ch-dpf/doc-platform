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
            Sheet ref = workbook.createSheet("Ref");
            ref.createRow(0).createCell(0).setCellValue(5);
            Sheet sheet = workbook.createSheet("Sales");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Region");
            header.createCell(1).setCellValue("Quarter");
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 1, 2));
            Row subHeader = sheet.createRow(1);
            subHeader.createCell(0).setCellValue("Region");
            subHeader.createCell(1).setCellValue("Q1");
            subHeader.createCell(2).setCellValue("Q2");
            Row data = sheet.createRow(2);
            data.createCell(0).setCellValue("APAC");
            data.createCell(1).setCellFormula("'Ref'!A1+5");
            sheet.addMergedRegion(new CellRangeAddress(2, 2, 1, 2));
            sheet.setColumnHidden(2, true);
            data.setZeroHeight(true);
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
        assertEquals(2, row.metadata().get("rowStart"));
        assertEquals(2, row.metadata().get("columnEnd"));
        assertEquals(true, row.metadata().get("hiddenRow"));
        assertEquals(List.of(2), row.metadata().get("hiddenColumns"));
        assertEquals(List.of("Ref"), row.metadata().get("crossSheetReferences"));
        assertTrue(row.content().contains("Quarter > Q1=10"));
        assertTrue((Boolean) row.metadata().get("hasMergedCells"));
        assertTrue(row.metadata().containsKey("mergedCells"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cells = (List<Map<String, Object>>) row.metadata().get("cellCoordinates");
        assertTrue(cells.stream().anyMatch(cell -> Boolean.TRUE.equals(cell.get("merged"))
                && "R3C2:R3C3".equals(cell.get("mergedRange"))));
        assertTrue(cells.stream().anyMatch(cell -> List.of("Quarter", "Q1").equals(cell.get("headerPath"))));
    }
}
